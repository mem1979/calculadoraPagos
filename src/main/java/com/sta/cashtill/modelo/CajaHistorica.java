// CajaHistorica.java
package com.sta.cashtill.modelo;

import java.math.*;
import java.util.*;
import java.util.Calendar;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.quartz.*;

import com.sta.cashtill.auxiliares.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "fecha, totalGeneral;" +
				"totalEntrada, totalSalida, balance;" +
				"historicoDeCaja")

@Tab(editors ="List, Charts",
	 properties="fecha, totalEntrada, totalSalida, balance, totalGeneral",
	 defaultOrder="${fecha} desc"			

		)


public class CajaHistorica extends Identifiable implements Job {

    @Column(nullable = false)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fecha;

    @Money
    @Depends("fecha")
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
    
    BigDecimal totalEntrada;

    @Money
    @Depends("fecha")
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
    
    BigDecimal totalSalida;

    @Money
    @Depends("calculaTotalEntrada, calculaTotalSalida, fecha")
    public BigDecimal getCalculaBalance() {
        return getCalculaTotalEntrada().add(getCalculaTotalSalida());
    }
    
    BigDecimal balance;

    @Money
    @Depends("calculaBalance, fecha")
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
    
    BigDecimal totalGeneral;
    
    @ElementCollection
    @ListProperties(value = "id, cantidad, total+")
	private List<HistoricoDeCaja> historicoDeCaja;

    @SimpleList
    @ListProperties("id, cantidad, total+")
    @ReadOnly // Marcar como solo lectura para que no intente persistir
    public List<HistoricoDeCaja> getHistoricoDeCajaCalculado() {
        List<HistoricoDeCaja> resultado = new ArrayList<>();
        try {
            // Realizar la consulta para obtener datos desde la base de datos
            Query query = XPersistence.getManager().createQuery(
                "FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC", Caja.class);
            List<Caja> cajas = query.getResultList();

            // Transformar los resultados a HistoricoDeCaja
            for (Caja caja : cajas) {
                HistoricoDeCaja historico = new HistoricoDeCaja();
                historico.setId(String.valueOf(caja.getId())); // Convertir ID a String
                historico.setCantidad(caja.getCantidad());
                historico.setTotal(caja.getTotal() != null ? caja.getTotal() : BigDecimal.ZERO);
                resultado.add(historico);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado; // Devolver la colección calculada
    }

    @PrePersist
    @PreUpdate
    private void calcularPropiedadesPersistentes() {
        setTotalEntrada(getCalculaTotalEntrada());
        setTotalSalida(getCalculaTotalSalida());
        setBalance(getCalculaBalance());
        setTotalGeneral(getCalculaTotalGeneral());
        setHistoricoDeCaja(getHistoricoDeCajaCalculado());
     //   calcularCajaOrdenada();
    }

 /*   private void calcularCajaOrdenada() {
        try {
            Query query = XPersistence.getManager().createQuery(
                "FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC", Caja.class);
            List<Caja> resultados = query.getResultList();

            historicoDeCaja.clear(); // Limpiar la colección existente
            for (Caja caja : resultados) {
                HistoricoDeCaja historico = new HistoricoDeCaja();
                historico.setId(String.valueOf(caja.getId())); // Convertir a String
                historico.setCantidad(caja.getCantidad());
                historico.setTotal(caja.getTotal() != null ? caja.getTotal() : BigDecimal.ZERO);
                historicoDeCaja.add(historico);
            }
        } catch (Exception e) {
            e.printStackTrace();
            historicoDeCaja.clear();
        }
    } */

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Ejecución del Job CajaHistorica: " + new Date());
        try {
            // Obtener fecha actual
            Calendar hoy = Calendar.getInstance();
            this.setFecha(hoy.getTime());

            // Persistir en la base de datos
            XPersistence.getManager().persist(this);

            System.out.println("CajaHistorica generada correctamente para la fecha: " + this.fecha);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}