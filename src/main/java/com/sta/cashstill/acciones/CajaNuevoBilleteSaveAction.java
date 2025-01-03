package com.sta.cashstill.acciones;

import org.openxava.actions.*;

public class CajaNuevoBilleteSaveAction  extends SaveAction {

    @Override
    public void execute() throws Exception {
        try {
            // Ejecuta la acción de guardado
            super.execute();
            // Verifica si hubo errores de validación
            if (!getErrors().isEmpty()) {
                return; // Si hay errores, no cierra el diálogo
            }
            // Si todo fue exitoso, cierra el diálogo
            closeDialog();
        } catch (Exception e) {
            // Maneja cualquier excepción y muestra el mensaje de error
            addError("Ocurrió un error al guardar el billete: " + e.getMessage());
        }
    }
}