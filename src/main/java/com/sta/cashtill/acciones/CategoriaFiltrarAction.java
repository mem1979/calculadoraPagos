package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CategoriaFiltrarAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        // Obtener el valor de tipoMovimiento desde la vista
        Object tipoMovimiento = getView().getValue("tipoMovimiento");

        // Validar que tipoMovimiento no sea nulo
        if (tipoMovimiento == null) {
            addError("Debe seleccionar un tipo de movimiento primero");
            return;
        }

        // Recargar la referencia movimientoCaja
        getView().refreshCollections();
    }
}