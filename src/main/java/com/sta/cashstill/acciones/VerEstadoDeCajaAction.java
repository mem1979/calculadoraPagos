package com.sta.cashstill.acciones;

import java.util.*;

import org.openxava.actions.*;

public class VerEstadoDeCajaAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
        	showDialog(); 
            // Cambiar al modelo `CajaEntrada`
        	 getView().setModelName("CajaDashboard");
        	 getView().setTitle("ESTADO DE CAJA ACTUAL");
        	 getView().setValueNotifying("fechaHora", new Date());
             getView().refreshCollections();
        	 
            
            } catch (Exception ex) {
            ex.printStackTrace();
            addError("system_error");
        }
    }
}