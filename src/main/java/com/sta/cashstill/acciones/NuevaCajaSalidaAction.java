package com.sta.cashstill.acciones;

import org.openxava.actions.*;

public class NuevaCajaSalidaAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
	        	showDialog(); 
	         // Cambiar al modelo `CajaSalida`
	        	getView().setModelName("CajaSalida");
	        	getView().setViewName("salida");
	        	getView().setTitle("SALIDA DE EFECTIVO - $");
	        	addActions("MovimientosDeCaja.ActualizarCajaSalida", "Dialog.cancel");
	                    
	            } catch (Exception ex) {
	            ex.printStackTrace();
	            addError("system_error");
        }
    }
}