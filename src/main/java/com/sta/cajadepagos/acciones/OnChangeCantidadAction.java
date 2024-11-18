package com.sta.cajadepagos.acciones;

import java.math.*;

import org.openxava.actions.*;

import com.sta.cajadepagos.modelo.*;

public class OnChangeCantidadAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        Caja caja = (Caja) getView().getEntity();
        
        // Si denominacion es nulo, no hacer nada
        if (caja.getDenominacion() == null || caja.getDenominacion().getValor() == null) {
             return;
        }
        
        if (caja.getDenominacion() != null && caja.getDenominacion().getValor() != null) {
            BigDecimal valor = caja.getDenominacion().getValor();
            Integer cantidad = (Integer) getNewValue();
            BigDecimal total = valor.multiply(BigDecimal.valueOf(cantidad != null ? cantidad : 0));
            getView().setValue("total", total);
        } else {
                getView().setValue("total", BigDecimal.ZERO);
        }
    }
}