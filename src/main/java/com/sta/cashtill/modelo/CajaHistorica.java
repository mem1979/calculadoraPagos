package com.sta.cashtill.modelo;

import java.math.*;
import java.time.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.quartz.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "fecha, totalEntrada, totalSalida, balance, totalGeneral")// ;" +
            //    "billetes { billetes }")
public class CajaHistorica extends Identifiable implements Job {

    @Column(nullable = false)
    private LocalDate fecha;

    @Money
    private BigDecimal totalEntrada;

    @Money
    private BigDecimal totalSalida;

    @Money
    private BigDecimal balance;

    @Money
    private BigDecimal totalGeneral;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "historica_id")  // FK en Caja que enlaza con CajaHistorica
    @ListProperties("denominacion.nombre, cantidad, total")
    private Collection<Caja> billetes;

    @Override
    public void execute(JobExecutionContext context) {
        // Lógica para generar la instancia diaria de CajaHistorica
        EntityManager em = XPersistence.getManager();
        try {
            System.out.println("Generando CajaHistorica...");

            // Obtener fecha actual
            LocalDate hoy = LocalDate.now();

            // Obtener billetes actuales
            Collection<Caja> billetes = em.createQuery(
                    "SELECT c FROM Caja c WHERE c.cantidad > 0", Caja.class).getResultList();

            // Configurar valores
            this.setFecha(hoy);
            this.setBilletes(billetes);
            this.calcularTotales();

            // Persistir en la base de datos
            em.getTransaction().begin();
            em.persist(this);
            em.getTransaction().commit();

            System.out.println("CajaHistorica generada correctamente para la fecha: " + hoy);

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }
    }

    @PrePersist
    @PreUpdate
    private void calcularTotales() {
        totalEntrada = BigDecimal.ZERO;
        totalSalida = BigDecimal.ZERO;

        try {
            BigDecimal entrada = (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(c.total), 0) FROM CajaEntrada c WHERE DATE(c.fechaHora) = :hoy")
                .setParameter("hoy", fecha)
                .getSingleResult();

            BigDecimal salida = (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(c.total), 0) FROM CajaSalida c WHERE DATE(c.fechaHora) = :hoy")
                .setParameter("hoy", fecha)
                .getSingleResult();

            this.totalEntrada = entrada != null ? entrada : BigDecimal.ZERO;
            this.totalSalida = salida != null ? salida.abs() : BigDecimal.ZERO;
            this.balance = totalEntrada.subtract(totalSalida);
            calcularTotalGeneral();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calcularTotalGeneral() {
        int yearActual = LocalDate.now().getYear();
        try {
            BigDecimal acumulado = (BigDecimal) XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(h.balance), 0) FROM CajaHistorica h WHERE YEAR(h.fecha) = :yearActual")
                .setParameter("yearActual", yearActual)
                .getSingleResult();

            this.totalGeneral = acumulado != null ? acumulado : BigDecimal.ZERO;
        } catch (Exception e) {
            e.printStackTrace();
            this.totalGeneral = BigDecimal.ZERO;
        }
    }
}
