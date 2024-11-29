package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaNuevoBilleteAction extends NewAction {
	
	@Override
    public void execute() throws Exception {
		super.execute();
		getView().setValue("id", "Nuevo");
		
	}
}
