package com.sta.cashstill.acciones;

import org.openxava.actions.*;

import com.sta.cashstill.modelo.*;

public class ActualizarCajaRegistrarMovimientoAction extends SaveAction {

    @Override
    public void execute() throws Exception {
     // Obtener la entidad principal desde la vista
        CajaRegistradora cajaRegistradora = (CajaRegistradora) getView().getEntity();
        

     // Determinar si es Entrada o Salida
        boolean esEntrada = cajaRegistradora instanceof CajaEntrada;

     // Guardar la entidad principal
        super.execute();
/*
        // Procesar los detalles
        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            Caja caja = XPersistence.getManager().find(Caja.class, detalle.getCaja().getId());
            if (caja == null) {
                addError("Caja no encontrada: " + detalle.getCaja().getId());
                continue;
            }

            try {
                // Actualizar caja según el tipo de movimiento
                caja.actualizarCaja(detalle.getCantidad(), detalle.getTotal(), esEntrada);
            } catch (IllegalStateException e) {
                addError(e.getMessage());
            }
        } */

        // Mensaje de éxito
        closeDialog();
        addMessage("Caja actualizada correctamente para el movimiento de " + getView().getValueString("movimientoCajaNombre") + ".");
    }
}