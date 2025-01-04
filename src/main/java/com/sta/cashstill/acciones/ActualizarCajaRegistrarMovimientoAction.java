package com.sta.cashstill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.modelo.*;

public class ActualizarCajaRegistrarMovimientoAction extends SaveAction {

    private boolean esVuelto = false; 

    @Override
    public void execute() throws Exception {
        // Obtener la entidad actual
        Object entidad = getView().getEntity();

        // Verificar si es una CajaEntrada con vuelto
        if (entidad instanceof CajaEntrada) {
            CajaEntrada entrada = (CajaEntrada) entidad;
            if (entrada.getVuelto() != null 
                && entrada.getVuelto().compareTo(BigDecimal.ZERO) > 0
                && !entrada.isConVuelto()) {

                // Cambiar controlador para registrar el vuelto
                addActions("CajaRegistradora.REGISTRAR VUELTO");
                getView().setHidden("conVuelto", false);
                addWarning("Se detectó un excedente. Registre primero la salida del vuelto.");
                return;
            }
        } 
        // Verificar si estamos procesando un vuelto
        else if (entidad instanceof CajaSalida) {
            MovimientoCaja movimientoCaja = XPersistence.getManager()
                .find(MovimientoCaja.class, getView().getValue("movimientoCaja.id"));

            if (movimientoCaja != null && movimientoCaja.getNombre().equalsIgnoreCase("VUELTO")) {
                getPreviousView().setValueNotifying("conVuelto", true);
                this.esVuelto = true;
            }
        }

        // Guardar la entidad (proceso normal)
        super.execute();

        // Detener si hubo errores
        if (!getErrors().isEmpty()) {
            return;
        }

        // Procesar la lógica específica según el tipo de entidad
        if (entidad instanceof CajaEntrada) {
            CajaEntrada entrada = (CajaEntrada) entidad;
            procesarMovimiento(entrada.getDetalle(), true);
        } 
        else if (entidad instanceof CajaSalida) {
            CajaSalida salida = (CajaSalida) entidad;
            procesarMovimiento(salida.getDetalle(), false);
        }

        // Cerrar el diálogo
        closeDialog();

    /*    // Mensajes finales según el flujo
        if (!esVuelto) {
            addMessage("Movimiento registrado y cajas actualizadas correctamente.");
        } else {
            // Ejecutar la acción de actualización de la entrada original
         //   executeAction("MovimientoCaja.ActualizarCaja");
            addMessage("Movimiento y vuelto registrado. Cajas actualizadas correctamente.");
        }*/
    }

    /**
     * Procesa el movimiento (entrada o salida) y actualiza las cajas.
     */
    private void procesarMovimiento(List<DetalleCajaRegistradora> detalles, boolean esEntrada) throws Exception {
        for (DetalleCajaRegistradora detalle : detalles) {
            Caja caja = XPersistence.getManager().find(Caja.class, detalle.getCaja().getId());
            if (caja == null) {
                throw new IllegalStateException("No se encontró una caja asociada en el detalle.");
            }

            int cantidadMovimiento = detalle.getCantidad();
            if (esEntrada) {
                caja.registrarEntrada(cantidadMovimiento);
            } else {
                caja.registrarSalida(cantidadMovimiento);
            }

            if (caja.getValor() == null) {
                throw new IllegalStateException("El valor unitario de la caja no puede ser nulo.");
            }

            caja.setTotal(caja.getValor().multiply(BigDecimal.valueOf(caja.getCantidad())));
            XPersistence.getManager().merge(caja);
        }
        XPersistence.getManager().flush();
    }
}
