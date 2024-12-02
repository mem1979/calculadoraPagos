package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaRegistradoraActualizarDetalleCategoriaMovimientoAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        // Obtenemos el valor de la referencia `movimientoCaja`
        Object movimientoCaja = getNewValue();

        // Verificamos si es nulo
        if (movimientoCaja == null) {
            // Limpiar la descripción si no hay movimientoCaja seleccionado
            getView().setValue("descripcion", "");
        } else {
            // Obtener la descripción del movimientoCaja y asignarla al campo `descripcion`
            String descripcionMovimientoCaja = (String) getView().getValue("descripcionMovimientoCaja");
            getView().setValue("descripcion", descripcionMovimientoCaja);
        }
    }
}