package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeCajaEntradaAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad CajaEntrada desde la vista
        CajaEntrada cajaEntrada = (CajaEntrada) getView().getEntity();

        if (cajaEntrada == null) {
            addError("No se encontró la entidad CajaEntrada. Asegúrate de que está correctamente configurada.");
            return;
        }

        // Validar que haya detalles para procesar
        if (cajaEntrada.getDetalle() == null || cajaEntrada.getDetalle().isEmpty()) {
            addError("No hay detalles en la colección para procesar.");
            return;
        }

        // Intentar guardar la entidad principal
        try {
            super.execute();
        } catch (Exception e) {
            addError("Error durante la ejecución de la acción base: " + e.getMessage());
            e.printStackTrace();
            return; // Detener si hay un error en el guardado
        }

        // Procesar cada detalle de la colección
        for (DetalleCajaRegistradora detalle : cajaEntrada.getDetalle()) {
            if (detalle == null) {
                addError("Uno de los detalles es nulo. Revisa la colección.");
                continue;
            }

            Caja caja = detalle.getCaja();
            if (caja == null) {
                addError("La relación Caja en el detalle es nula.");
                continue;
            }

            String cajaId = caja.getId();
            if (cajaId == null || cajaId.isEmpty()) {
                addError("El ID de la caja en el detalle es nulo o vacío.");
                continue;
            }

            // Buscar la caja persistida por ID
            Caja cajaPersistida = buscarCajaPorId(cajaId);
            if (cajaPersistida == null) {
                addError("No se encontró una caja con el ID: " + cajaId);
                continue;
            }

            // Validar valores actuales de la caja antes de operar
            if (cajaPersistida.getCantidad() == 0) {
                cajaPersistida.setCantidad(0); // Inicializar cantidad a 0 si es null
            }
            if (cajaPersistida.getTotal() == null) {
                cajaPersistida.setTotal(BigDecimal.ZERO); // Inicializar total a 0 si es null
            }

            // Actualizar los valores de la caja con los datos del detalle
            int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
            BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

            cajaPersistida.setCantidad(cajaPersistida.getCantidad() + cantidad);
            cajaPersistida.setTotal(cajaPersistida.getTotal().add(total));

            // Persistir los cambios en la caja
            try {
                XPersistence.getManager().merge(cajaPersistida);
            } catch (Exception e) {
                addError("Error al actualizar la caja con ID " + cajaId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Cerrar el diálogo y mostrar un mensaje si no hay errores
        if (getErrors().isEmpty()) {
            closeDialog();
            addMessage("Caja de entrada actualizada correctamente.");
        } else {
            addError("Ocurrieron errores durante la actualización de las cajas.");
        }
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
