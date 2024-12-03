package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaSalidaAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
        	showDialog(); 
            // Cambiar al modelo `CajaSalida`
        	 getView().setModelName("CajaSalida");
        	 getView().setViewName("salida");
        	 getView().setTitle("SALIDA DE EFECTIVO - $");
        	 addActions("MovimientoCaja.ActualizarCajaSalida", "Dialog.cancel");;
                    
            } catch (Exception ex) {
            ex.printStackTrace();
            addError("system_error");
        }
    }
}