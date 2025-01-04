package com.sta.cashstill.acciones;

import org.openxava.actions.*;

public class CajaNuevoBilleteSaveAction  extends SaveAction {

    @Override
    public void execute() throws Exception {
        try {
            // Ejecuta la acci�n de guardado
            super.execute();
            // Verifica si hubo errores de validaci�n
            if (!getErrors().isEmpty()) {
                return; // Si hay errores, no cierra el di�logo
            }
            // Si todo fue exitoso, cierra el di�logo
            closeDialog();
        } catch (Exception e) {
            // Maneja cualquier excepci�n y muestra el mensaje de error
            addError("Ocurri� un error al guardar el billete: " + e.getMessage());
        }
    }
}