package com.sta.cajadepagos.acciones;

import java.math.*;

import org.openxava.actions.*;

import com.sta.cajadepagos.modelo.*;

public class OnChangeTotalAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        Caja caja = (Caja) getView().getEntity();
        BigDecimal total = (BigDecimal) getNewValue();

        // Si denominacion es nulo, no hacer nada
        if (caja.getDenominacion() == null || caja.getDenominacion().getValor() == null) {
             return;
        }

        BigDecimal valor = caja.getDenominacion().getValor();

        // Validar que total sea un múltiplo de denominacion.valor
        if (total != null && valor.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainder = total.remainder(valor);
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                addError(
                    "total_no_valido",
                    "total",
                    valor,
                    valor,
                    valor.multiply(BigDecimal.valueOf(2)),
                    valor.multiply(BigDecimal.valueOf(3))
                );
                return;
            }
        }

        // Actualizar cantidad si todo es válido
        if (caja.getDenominacion() != null && caja.getDenominacion().getValor() != null && total != null) {
            int cantidad = total.divide(valor).intValue();
            getView().setValue("cantidad", cantidad);
        }
    }
}

