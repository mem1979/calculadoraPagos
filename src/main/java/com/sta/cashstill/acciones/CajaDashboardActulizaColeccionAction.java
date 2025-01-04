package com.sta.cashstill.acciones;

import org.openxava.actions.*;

public class CajaDashboardActulizaColeccionAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
    	
    	getView().refreshCollections();

    }
}