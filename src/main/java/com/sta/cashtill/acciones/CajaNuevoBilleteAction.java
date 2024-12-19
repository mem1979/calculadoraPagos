package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaNuevoBilleteAction extends ViewBaseAction {
	
	@Override
    public void execute() throws Exception {
		 try {
	        	showDialog(); 
	            // Cambiar al modelo `Caja`
	        	 getView().setModelName("Caja");
	        	 getView().setTitle("Nueva denominacion de Billete en Caja");
	        	 getView().setValue("id", "Nuevo");
	        	 setControllers("Caja");
	            
	            } catch (Exception ex) {
	            ex.printStackTrace();
	            addError("system_error");
	        }
	    }
		
}
