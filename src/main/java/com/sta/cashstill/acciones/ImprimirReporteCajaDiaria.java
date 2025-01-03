package com.sta.cashstill.acciones;

import java.util.*;

import javax.inject.*;
import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;
import org.openxava.tab.*;

import com.sta.cashstill.modelo.*;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.*;

public class ImprimirReporteCajaDiaria extends JasperReportBaseAction {

    @Inject
    private Tab tab; // Tab inyectado para acceder a las filas seleccionadas o visibles

    @Override
    protected JRDataSource getDataSource() throws Exception {
        List<CajaRegistradora> listaCajas = new ArrayList<>();
        EntityManager em = XPersistence.getManager();

        if (tab.getSelectedKeys().length > 0) {
            // Si hay filas seleccionadas, cargar todas las propiedades explícitamente
            for (Map<?, ?> key : tab.getSelectedKeys()) {
                CajaRegistradora caja = em.createQuery(
                        "SELECT c FROM CajaRegistradora c WHERE c.id = :id", CajaRegistradora.class)
                        .setParameter("id", key.get("id"))
                        .getSingleResult();
                listaCajas.add(caja);
            }
        } else {
            // Si no hay selección, cargar todas las filas de la fecha actual
            Date fechaActual = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaActual);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date inicioDia = cal.getTime();

            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date finDia = cal.getTime();

            listaCajas = em.createQuery(
                "SELECT c FROM CajaRegistradora c WHERE c.fechaHora >= :inicioDia AND c.fechaHora < :finDia", 
                CajaRegistradora.class)
                .setParameter("inicioDia", inicioDia)
                .setParameter("finDia", finDia)
                .getResultList();
        }

        // Retorna el DataSource asegurando que todas las columnas sean cargadas
        return new JRBeanCollectionDataSource(listaCajas, false);
    }

    @Override
    protected String getJRXML() throws Exception {
        return "CajaRegistradoraReporte.jrxml";
    }

    @Override
    protected Map<String, Object> getParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("titulo", "Reporte de Caja Registradora");
        params.put("fecha", new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        return params;
    }
}
