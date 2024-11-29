package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaEntradaAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
        	showDialog(); 
            // Cambiar al modelo `CajaEntrada`
        	 getView().setModelName("CajaEntrada");
        	 getView().setTitle("INGRESO DE EFECTIVO + $");
        	 setControllers("MovimientoCaja");
            
            } catch (Exception ex) {
            ex.printStackTrace();
            addError("system_error");
        }
    }

	
	}