package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeCajaSalidaAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad principal
        CajaSalida cajaSalida = (CajaSalida) getView().getEntity();

        if (cajaSalida == null) {
            addError("No se encontró la entidad CajaSalida.");
            return;
        }

        // Validar cantidades antes de ejecutar el movimiento
        if (!validarCajas(cajaSalida)) {
            addError("No hay suficiente cantidad en una o más cajas para realizar el movimiento.");
            return;
        }

        // Intentar guardar la entidad principal
        try {
            super.execute();
        } catch (Exception e) {
            addError("Error durante la ejecución de la acción: " + e.getMessage());
            e.printStackTrace();
            return; // Detener si ocurre un error
        }

        // Procesar los detalles solo si no hubo errores
        if (getErrors().isEmpty()) {
            for (DetalleCajaRegistradora detalle : cajaSalida.getDetalle()) {
                if (detalle == null) {
                    addError("El detalle es nulo. Revisa la configuración.");
                    continue;
                }

                // Validar y buscar la caja
                Caja caja = buscarCajaPorId(detalle.getCaja().getId());
                if (caja == null) {
                    addError("No se encontró una caja con el ID: " + detalle.getCaja().getId());
                    continue;
                }

                // Validar valores actuales antes de restar
                if (caja.getCantidad() == 0) {
                    caja.setCantidad(0); // Inicializar si es null
                }
                if (caja.getTotal() == null) {
                    caja.setTotal(BigDecimal.ZERO); // Inicializar si es null
                }

                // Actualizar los valores de la caja
                int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
                BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

                if (cantidad > caja.getCantidad()) {
                    addError("La cantidad en la caja con ID " + caja.getId() + " es insuficiente.");
                    continue;
                }

                caja.setCantidad(caja.getCantidad() - cantidad);
                caja.setTotal(caja.getTotal().subtract(total));

                // Persistir los cambios
                try {
                    XPersistence.getManager().merge(caja);
                } catch (Exception e) {
                    addError("Error al actualizar la caja con ID " + caja.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Cerrar diálogo y mostrar mensaje de éxito si no hubo errores
            if (getErrors().isEmpty()) {
                closeDialog();
                addMessage("Caja de salida actualizada correctamente.");
            } else {
                addError("Hubo errores durante la actualización de las cajas.");
            }
        }
    }

    /**
     * Valida que las cajas tengan suficiente cantidad antes de realizar el movimiento.
     *
     * @param cajaSalida Entidad principal con los detalles a validar.
     * @return true si todas las cajas tienen suficiente cantidad, false en caso contrario.
     */
    private boolean validarCajas(CajaSalida cajaSalida) {
        for (DetalleCajaRegistradora detalle : cajaSalida.getDetalle()) {
            if (detalle == null) continue;

            Caja caja = buscarCajaPorId(detalle.getCaja().getId());
            if (caja == null || caja.getCantidad() == 0 || caja.getCantidad() < detalle.getCantidad()) {
                addError("La caja con ID " + detalle.getCaja().getId() + " no tiene suficiente cantidad.");
                return false;
            }
        }
        return true;
    }

    /**
     * Busca una caja en la base de datos utilizando su ID.
     *
     * @param cajaId ID de la caja a buscar.
     * @return Instancia de Caja encontrada o null si no existe.
     */
    private Caja buscarCajaPorId(String cajaId) {
        try {
            return XPersistence.getManager()
                    .createQuery("SELECT c FROM Caja c WHERE c.id = :cajaId", Caja.class)
                    .setParameter("cajaId", cajaId)
                    .getSingleResult();
        } catch (Exception e) {
            System.err.println("Error al buscar la caja con ID: " + cajaId + ". Excepción: " + e.getMessage());
            return null;
        }
    }
}
