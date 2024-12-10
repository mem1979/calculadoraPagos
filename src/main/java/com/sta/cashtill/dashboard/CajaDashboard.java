package com.sta.cashtill.dashboard;

import java.util.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;

import com.sta.cashtill.enums.*;

@View(members = 
    "totalesPorCategoria;"+
    "totalesPorUsuario;"+
    "distribucionPorTipoMovimiento;"+
    "evolucionTemporalPorMes ")

public class CajaDashboard {

    @Chart // Gráfico de barras: Totales por categoría
    public Collection<ResumenPorCategoria> getTotalesPorCategoria() {
        String jpql = "SELECT new com.sta.cashtill.dashboard.ResumenPorCategoria(c.categoria, SUM(c.total)) " +
                      "FROM CajaRegistradora c GROUP BY c.categoria";
        return XPersistence.getManager().createQuery(jpql, ResumenPorCategoria.class).getResultList();
    }

    @Chart // Gráfico de barras: Totales por usuario
    public Collection<ResumenPorUsuario> getTotalesPorUsuario() {
        String jpql = "SELECT new com.sta.cashtill.dashboard.ResumenPorUsuario(c.usuario, SUM(c.total)) " +
                      "FROM CajaRegistradora c GROUP BY c.usuario";
        return XPersistence.getManager().createQuery(jpql, ResumenPorUsuario.class).getResultList();
    }

    @Chart // Gráfico circular: Distribución por tipo de movimiento
    public Collection<DistribucionPorTipoMovimiento> getDistribucionPorTipoMovimiento() {
        String jpql = "SELECT new com.sta.cashtill.dashboard.DistribucionPorTipoMovimiento(c.tipoMovimiento, SUM(c.total)) " +
                      "FROM CajaRegistradora c GROUP BY c.tipoMovimiento";
        return XPersistence.getManager().createQuery(jpql, DistribucionPorTipoMovimiento.class).getResultList();
    }

    @Chart // Gráfico de líneas: Evolución temporal por mes
    public Collection<EvolucionTemporal> getEvolucionTemporalPorMes() {
        String jpql = "SELECT new com.sta.cashtill.dashboard.EvolucionTemporal(" +
                      "YEAR(c.fechaHora), MONTH(c.fechaHora), " +
                      "SUM(CASE WHEN c.tipoMovimiento = :entrada THEN c.total ELSE 0 END), " +
                      "SUM(CASE WHEN c.tipoMovimiento = :salida THEN c.total ELSE 0 END)) " +
                      "FROM CajaRegistradora c " +
                      "GROUP BY YEAR(c.fechaHora), MONTH(c.fechaHora) " +
                      "ORDER BY YEAR(c.fechaHora), MONTH(c.fechaHora)";
        return XPersistence.getManager()
            .createQuery(jpql, EvolucionTemporal.class)
            .setParameter("entrada", TipoMovimiento.ENTRADA)
            .setParameter("salida", TipoMovimiento.SALIDA)
            .getResultList();
    }

}
