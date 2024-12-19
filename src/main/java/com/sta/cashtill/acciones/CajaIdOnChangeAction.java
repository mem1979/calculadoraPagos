package com.sta.cashtill.acciones;

import java.math.*;

import org.openxava.actions.*;

public class CajaIdOnChangeAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        
    	int filaActual = getView().getCollectionEditingRow();
    	System.out.println(filaActual);
    	
            getView().setValue("total", BigDecimal.ZERO);
            getView().setValue("cantidad", "");
            getView().setFocus("detalle.0.cantidad");
        
    }
}
