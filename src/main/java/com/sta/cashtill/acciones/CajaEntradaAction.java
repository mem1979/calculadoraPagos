package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaEntradaAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
        	showDialog(); 
            // Cambiar al modelo `CajaEntrada`
        	 getView().setModelName("CajaEntrada");
        	 getView().setViewName("entrada");
        	 getView().setTitle("INGRESO DE EFECTIVO + $");
        	 addActions("MovimientoCaja.ActualizarCajaEntrada", "Dialog.cancel");
            
            } catch (Exception ex) {
            ex.printStackTrace();
            addError("system_error");
        }
    }

	
	}