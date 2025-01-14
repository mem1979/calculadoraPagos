// CajaHistorica.java
package com.sta.cashstill.modelo;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.Calendar;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.quartz.*;

import com.sta.cashstill.auxiliares.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "fecha, totalGeneral;" +
                "totalEntrada, totalSalida, balance;" +
                "historicoDeCaja")

@Tab(editors ="List, Charts",
     properties="fechaCierre, totalEntrada+, totalSalida+, balance+, totalGeneral+",
     defaultOrder="${fecha} desc")


public class CajaHistorica extends Identifiable implements Job {

	@DateTime
	@LargeDisplay(icon = "calendar-clock")
	@Column(nullable = false)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fecha;
	
	@DefaultValueCalculator(CurrentLocalDateCalculator.class)
    private LocalDate fechaCierre;

    @Money
    public BigDecimal getCalculaTotalEntrada() {
        try {
            return (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(e.total), 0) FROM CajaEntrada e WHERE e.fechaHora >= :startOfDay AND e.fechaHora < :endOfDay")
                .setParameter("startOfDay", getStartOfDay(fecha))
                .setParameter("endOfDay", getEndOfDay(fecha))
                .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    @Money
    @LargeDisplay(icon = "cash-plus")
    private BigDecimal totalEntrada;

    @Money
    public BigDecimal getCalculaTotalSalida() {
        try {
            return (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(s.total), 0) FROM CajaSalida s WHERE s.fechaHora >= :startOfDay AND s.fechaHora < :endOfDay")
                .setParameter("startOfDay", getStartOfDay(fecha))
                .setParameter("endOfDay", getEndOfDay(fecha))
                .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    @Money
    @LargeDisplay(icon = "cash-minus")
    private BigDecimal totalSalida;

    @Money
    public BigDecimal getCalculaBalance() {
        return getCalculaTotalEntrada().add(getCalculaTotalSalida());
    } 

    @Money
    @LargeDisplay(icon = "cash-multiple")
    private BigDecimal balance;
    
    @Money
    public BigDecimal getCalculaTotalGeneral() {
        Calendar yearStart = Calendar.getInstance();
        yearStart.setTime(this.fecha);
        yearStart.set(Calendar.MONTH, Calendar.JANUARY);
        yearStart.set(Calendar.DAY_OF_MONTH, 1);
        yearStart.set(Calendar.HOUR_OF_DAY, 0);
        yearStart.set(Calendar.MINUTE, 0);
        yearStart.set(Calendar.SECOND, 0);
        yearStart.set(Calendar.MILLISECOND, 0);

        Calendar yearEnd = (Calendar) yearStart.clone();
        yearEnd.add(Calendar.YEAR, 1);

        try {
            BigDecimal acumulado = (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(h.balance), 0) FROM CajaHistorica h WHERE h.fecha >= :yearStart AND h.fecha < :yearEnd")
                .setParameter("yearStart", yearStart.getTime())
                .setParameter("yearEnd", yearEnd.getTime())
                .getSingleResult();

            return acumulado != null ? acumulado : BigDecimal.ZERO;
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    @Money
    @LargeDisplay(icon = "cash-register")
    private BigDecimal totalGeneral;

    @ReadOnly
    @ElementCollection
    @ListProperties(value = "denominacion, cantidad, total+")
    private List<HistoricoDeCaja> historicoDeCaja;

    @SuppressWarnings("unchecked")
    @SimpleList
    @ListProperties("denominacion, cantidad, total+")
    @ReadOnly
    public List<HistoricoDeCaja> getHistoricoDeCajaCalculado() {
        List<HistoricoDeCaja> resultado = new ArrayList<>();
        try {
            Query query = XPersistence.getManager().createQuery(
                "FROM Caja c WHERE c.cantidad > 0 ORDER BY c.valor DESC", Caja.class);
            List<Caja> cajas = query.getResultList();

            for (Caja caja : cajas) {
                HistoricoDeCaja historico = new HistoricoDeCaja();
                historico.setDenominacion(String.valueOf(caja.getDenominacion()));
                historico.setCantidad(caja.getCantidad());
                historico.setTotal(caja.getTotal() != null ? caja.getTotal() : BigDecimal.ZERO);
                resultado.add(historico);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }


    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Ejecución del Job CajaHistorica: " + new Date());
        EntityManagerFactory emf = null;
        EntityManager em = null;

        try {
            // Crear una instancia temporal para calcular los valores
            CajaHistorica cajaTemporal = new CajaHistorica();
            cajaTemporal.setFecha(new Date());
            
            BigDecimal totalEntrada = cajaTemporal.getCalculaTotalEntrada();
            BigDecimal totalSalida = cajaTemporal.getCalculaTotalSalida();
          
            // Log para depuración
            System.out.println("Total Entrada calculado: " + totalEntrada);
            System.out.println("Total Salida calculado: " + totalSalida);

            // Validar si ambos valores son cero o nulos
            if ((totalEntrada == null || totalEntrada.compareTo(BigDecimal.ZERO) == 0) &&
                (totalSalida == null || totalSalida.compareTo(BigDecimal.ZERO) == 0)) {
                System.out.println("Los valores de entrada o salida son cero o nulos. El método no continuará.");
                return; // Salir del método
            }

            emf = Persistence.createEntityManagerFactory("default");
            em = emf.createEntityManager();
            em.getTransaction().begin();

            CajaHistorica cajaHistorica = new CajaHistorica();
            cajaHistorica.setFecha(new Date());
            cajaHistorica.setFechaCierre(LocalDate.now());
            cajaHistorica.setTotalEntrada(totalEntrada);
            cajaHistorica.setTotalSalida(totalSalida);
            cajaHistorica.setBalance(cajaHistorica.getCalculaBalance());
            cajaHistorica.setTotalGeneral(cajaHistorica.getCalculaTotalGeneral());
            cajaHistorica.setHistoricoDeCaja(cajaHistorica.getHistoricoDeCajaCalculado());

            em.persist(cajaHistorica);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }
        }
    }
    
    
// Metodos Auxiliares para calcular fechas y horas
    private Date getStartOfDay(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
