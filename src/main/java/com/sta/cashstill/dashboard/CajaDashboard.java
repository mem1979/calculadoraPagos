package com.sta.cashstill.dashboard;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;

import com.sta.cashstill.acciones.*;
import com.sta.cashstill.modelo.*;

import lombok.*;

@View(members = "fechaHora, totalCaja;" +
		"DetalleDeCaja [totalEntradas, totalSalidas, balance; cajaOrdenada, distribucionPorTipoMovimiento]")

@Getter @Setter
public class CajaDashboard {

    @DateTime
   	@LargeDisplay(icon = "calendar-clock")
    @DefaultValueCalculator(CurrentDateCalculator.class)
    @OnChange(CajaDashboardActulizaColeccionAction.class)
    public Date getFechaHora() {
   		return new Date(); // Retorna la fecha/hora actual del sistema en el momento de la invocación
    }
   	
    @Money
    @Depends("fechaHora") // Dependencia de la colección cajaOrdenada
    @LargeDisplay(icon = "cash-multiple")
    public BigDecimal getTotalCaja() {
        Collection<Caja> cajaOrdenada = getCajaOrdenada(); // Obtener la colección calculada
        if (cajaOrdenada == null || cajaOrdenada.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Sumar el total de cada elemento en la colección
        BigDecimal total = cajaOrdenada.stream()
                                       .map(Caja::getTotal) // Obtener el total de cada Caja
                                       .filter(Objects::nonNull) // Evitar valores nulos
                                       .reduce(BigDecimal.ZERO, BigDecimal::add); // Sumar totales
        
        return total;
    }
   	

    @Money
    @LargeDisplay
    @Depends("fechaHora, distribucionPorTipoMovimiento")
    public BigDecimal getTotalEntradas() {
        if (getFechaHora() == null) return BigDecimal.ZERO;

        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(getFechaHora());
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(getFechaHora());
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        BigDecimal total = (BigDecimal) XPersistence.getManager()
            .createQuery("SELECT SUM(c.total) FROM CajaRegistradora c WHERE TYPE(c) = CajaEntrada " +
                         "AND c.fechaHora BETWEEN :start AND :end")
            .setParameter("start", startOfDay.getTime())
            .setParameter("end", endOfDay.getTime())
            .getSingleResult();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Money
    @LargeDisplay
    @Depends("fechaHora, distribucionPorTipoMovimiento")
    public BigDecimal getTotalSalidas() {
        if (getFechaHora() == null) return BigDecimal.ZERO;

        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(getFechaHora());
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(getFechaHora());
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        BigDecimal total = (BigDecimal) XPersistence.getManager()
            .createQuery("SELECT SUM(c.total) FROM CajaRegistradora c WHERE TYPE(c) = CajaSalida " +
                         "AND c.fechaHora BETWEEN :start AND :end")
            .setParameter("start", startOfDay.getTime())
            .setParameter("end", endOfDay.getTime())
            .getSingleResult();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Money
    @LargeDisplay
    @Depends("totalSalidas, totalEntradas, fechaHora")
    public BigDecimal getBalance() {
        if (getFechaHora() == null) return BigDecimal.ZERO;
        return getTotalEntradas().add(getTotalSalidas());
    }

    @SuppressWarnings("unchecked")
    @ReadOnly
    @SimpleList
    @ListProperties("denominacion, cantidad, total+")
    public Collection<Caja> getCajaOrdenada() {
        Query query = XPersistence.getManager()
            .createQuery("FROM Caja c WHERE c.cantidad > 0 ORDER BY c.valor DESC");
        return query.getResultList();
    }

 
    @ReadOnly
    @Chart // Gráfico circular: Distribución por tipo de movimiento para la fecha actual
    public Collection<DistribucionPorTipoMovimiento> getDistribucionPorTipoMovimiento() {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        // Aplicar ABS() al total para convertirlo en positivo
        String jpql = "SELECT new com.sta.cashstill.dashboard.DistribucionPorTipoMovimiento(c.tipoMovimiento, SUM(ABS(c.total))) " +
                      "FROM CajaRegistradora c " +
                      "WHERE c.fechaHora BETWEEN :start AND :end " +
                      "GROUP BY c.tipoMovimiento";

        return XPersistence.getManager()
            .createQuery(jpql, DistribucionPorTipoMovimiento.class)
            .setParameter("start", startOfDay.getTime())
            .setParameter("end", endOfDay.getTime())
            .getResultList();
    }
}