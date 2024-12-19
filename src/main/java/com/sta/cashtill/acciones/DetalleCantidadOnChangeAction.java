package com.sta.cashtill.acciones;

import java.math.*;

import org.openxava.actions.*;

import com.sta.cashtill.modelo.*;

public class DetalleCantidadOnChangeAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        DetalleCajaRegistradora detalle = (DetalleCajaRegistradora) getView().getEntity();
        Integer cantidad = (Integer) getNewValue();

        if (cantidad != null && detalle.getCaja() != null && detalle.getCaja().getDenominacion() != null) {
            BigDecimal valorDenominacion = detalle.getCaja().getDenominacion().getValor();
            BigDecimal total = valorDenominacion.multiply(BigDecimal.valueOf(cantidad));
            detalle.setTotal(total);
            getView().setValue("total", total);
        } else {
            getView().setValue("total", BigDecimal.ZERO);
        }
    }
}