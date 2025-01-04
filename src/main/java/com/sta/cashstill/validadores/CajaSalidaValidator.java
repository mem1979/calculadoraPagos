package com.sta.cashstill.validadores;

import java.math.*;
import java.util.*;

import org.openxava.jpa.*;
import org.openxava.util.*;
import org.openxava.validators.*;

import com.sta.cashstill.modelo.*;

import lombok.*;
/**
 * Validador para CajaSalida. Verifica que:
 * - 'detalle' no sea nulo/vacío.
 * - 'importe' > 0.
 * - 'importe' == 'totalDetalle'.
 * - Cada detalle tenga cantidades válidas y suficientes en Caja.
 */
@Getter @Setter
public class CajaSalidaValidator implements IValidator {

    private static final long serialVersionUID = 1L;

// Propiedades que recibimos de CajaSalida
    
    private Collection<?> detalle;
    private BigDecimal importe;
    private BigDecimal totalDetalle;

    @Override
    public void validate(Messages errors) throws Exception {

        // 1) Validar que 'detalle' no sea nulo ni vacío
        if (detalle == null || detalle.isEmpty()) {
            errors.add("Debe completar el detalle para registrar la salida de Efectivo", "detalle");
            return;
        }

        // 2) Validar que 'importe' > 0
        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("El importe de la Salida a registrar, debe ser mayor que $0.00- ", "importe");
        }

        // 3) Validar que 'importe' sea igual a 'totalDetalle'
           if (importe != null && totalDetalle != null && importe.compareTo(totalDetalle.abs()) != 0) {
            BigDecimal diferencia = totalDetalle.abs().subtract(importe);

            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                errors.add(
                	String.format(
                       "La Cantidad ingresada en el detalle, excede el Importe a pagar por %s.",
                       diferencia.abs()
                    ),
                    "importe"
                );
            } else {
                errors.add(
                    String.format(
                       "La Cantidad ingresada en el detalle, es menor al Importe a pagar por %s.",
                        diferencia.abs()
                    ),
                    "importe"
                );
            }
        }

        // 4) Validar cada elemento del detalle (HashMap)
        for (Object obj : detalle) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;

                // Extraer valores del HashMap
                Object cajaData = map.get("caja");
                Integer cantidad = (Integer) map.get("cantidad");
                BigDecimal total = (BigDecimal) map.get("total");

                // Validar que exista información sobre la caja
                if (cajaData == null) {
                    errors.add("Cada detalle debe indicar una Denominacion válida.", "detalle.caja");
                    continue;
                }

                // Recuperar la entidad Caja desde el ID contenido en el HashMap
                Caja caja = null;
                if (cajaData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cajaMap = (Map<String, Object>) cajaData;
                    String cajaId = (String) cajaMap.get("id");
                    if (cajaId != null) {
                        caja = XPersistence.getManager().find(Caja.class, cajaId);
                    }
                } else if (cajaData instanceof String) {
                    caja = XPersistence.getManager().find(Caja.class, cajaData);
                }

                // Validar que se haya encontrado la entidad Caja
                if (caja == null) {
                    errors.add("No se encontró el tipo de Billete ingresado.", "detalle.caja");
                    continue;
                }

                // Validar que la cantidad sea mayor que 0
                if (cantidad == null || cantidad <= 0) {
                    errors.add(
                        String.format("La cantidad especificada para la caja %s no es válida.", caja.getDenominacion()),
                        "detalle.cantidad"
                    );
                    continue;
                }

                // Validar que la cantidad disponible en Caja sea suficiente
                if (caja.getCantidad() < cantidad) {
                    errors.add(
                        String.format(
                            "La caja %s no tiene suficientes billetes/monedas disponibles. Solicitados: %d, Disponibles: %d.",
                            caja.getDenominacion(),
                            caja.getCantidad(),
                            cantidad
                        ),
                        "detalle.caja.cantidad"
                    );
                }

                // Validar que el total sea mayor que 0 (opcional)
                if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(
                        String.format("El total para la caja %s debe ser mayor que 0.", caja.getDenominacion()),
                        "detalle.total"
                    );
                }
            } else {
                // Si no es un HashMap, reportar como error
                errors.add("El detalle contiene un elemento no válido.", "detalle");
            }
        }
    }
}
