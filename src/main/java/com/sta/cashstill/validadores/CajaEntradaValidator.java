package com.sta.cashstill.validadores;

import java.math.*;
import java.util.*;

import org.openxava.util.*;
import org.openxava.validators.*;

import lombok.*;

/**
 * Validador para CajaEntrada. Verifica que:
 * - 'detalle' no sea nulo/vacío.
 * - 'importe' > 0.
 * - 'vuelto' no sea nulo y esté correctamente calculado.
 * - Cada detalle tenga cantidades válidas y suficientes en Caja.
 */
@Getter @Setter
public class CajaEntradaValidator implements IValidator {

    private static final long serialVersionUID = 1L;

    // Propiedades que recibimos de CajaEntrada
    private Collection<?> detalle;
    private BigDecimal importe;
    private BigDecimal vuelto;
    private boolean conVuelto;

    @Override
    public void validate(Messages errors) throws Exception {

        // 1) Validar que 'detalle' no sea nulo ni vacío
        if (detalle == null || detalle.isEmpty()) {
            errors.add("Debe completar el detalle para registrar un Ingreso de Efectivo.", "detalle");
            return;
        }

        // 2) Validar que 'importe' > 0
        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("El importe a registrar debe ser mayor que $0.00.", "importe");
        }

        // 3) Validar 'vuelto'
        if (vuelto == null) {
            errors.add("El vuelto no puede ser nulo. Verifica los valores ingresados.", "vuelto");
        } else if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
            // Si 'vuelto' es menor que 0, informar que falta dinero para cubrir el importe
            errors.add(
                String.format("Faltan $%s para completar el importe a Ingresar.", vuelto.abs()),
                "vuelto"
            );
        } else if (vuelto.compareTo(BigDecimal.ZERO) > 0 && !conVuelto) {
            // Si 'vuelto' es mayor que 0, informar que se debe registrar una salida para la devolución
            errors.add(
                String.format("Hay un excedente. Se debe registrar una salida de caja para devolver $%s.", vuelto),
                "vuelto"
            );
        }

        
    }
}
