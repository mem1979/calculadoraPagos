package com.sta.cashstill.acciones;

import java.math.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

public class NuevaCajaSalidaVueltoAction extends ViewBaseAction {

	@Override
    public void execute() throws Exception {
        try {
        	
        	removeActions("CajaRegistradora.REGISTRAR VUELTO");
        //	addActions("MovimientoCaja.ActualizarCaja");
        	
        	String MovimientoId = (String) XPersistence.getManager()
        		    .createQuery("SELECT c.id FROM MovimientoCaja c WHERE c.nombre = 'VUELTO'") // Consulta JPQL
        		    .getSingleResult();
        	
        	BigDecimal vuelto = (BigDecimal) getView().getValue("vuelto");
        	
        	 showDialog();
             getView().setModelName("CajaSalida");
             getView().setViewName("salida");
             getView().setTitle("SALIDA DE EFECTIVO - VUELTO");
             
             getView().setValueNotifying("movimientoCaja.id", MovimientoId); // Asignar el ID de "VUELTO"
             getView().setEditable("movimientoCaja", false);
             
             getView().setValue("importe", vuelto);
             getView().setEditable("importe", false);
	         addActions("MovimientoCaja.ActualizarCaja", "Dialog.cancel");
	                    
	            } catch (Exception ex) {
	            ex.printStackTrace();
	            addError("system_error");
        }
    }
}