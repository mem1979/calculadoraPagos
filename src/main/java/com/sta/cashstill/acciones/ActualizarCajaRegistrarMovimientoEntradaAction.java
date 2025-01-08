package com.sta.cashstill.acciones;

import java.math.*;

import com.sta.cashstill.modelo.*;

public class ActualizarCajaRegistrarMovimientoEntradaAction extends BaseActualizarCajaAction {

    @Override
    public void execute() throws Exception {
        
        // 1. Obtener la entidad actual desde la vista
        Object entidad = getView().getEntity();
        if (!(entidad instanceof CajaEntrada)) {
            // En teoría esto no debería pasar si el controller está bien configurado
            addError("No es una CajaEntrada");
            return;
        } 
        
        CajaEntrada entrada = (CajaEntrada) entidad;
        
        // 2. Verificar si hay un vuelto
        if (entrada.getVuelto() != null 
            && entrada.getVuelto().compareTo(BigDecimal.ZERO) > 0 
            && !entrada.isConVuelto()) {
            
            // 2.1. Redirigir para registrar el vuelto
            addActions("CajaRegistradora.REGISTRAR VUELTO");
            addWarning("Se detectó un excedente. Registre primero la salida del vuelto.");
            return; // Importante: no guardamos si hay vuelto sin registrar
        }

        // 3. Guardar la entidad (proceso normal)
        super.execute();

        // 4. Detener si hubo errores
        if (!getErrors().isEmpty()) {
            return;
        }

        // 5. Procesar el detalle (ahora que está guardado)
        procesarMovimiento(entrada.getDetalle(), true);

        // 6. Cerrar diálogo
        closeDialog();
    }
}
