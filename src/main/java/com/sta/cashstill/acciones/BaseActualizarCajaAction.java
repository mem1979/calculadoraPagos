package com.sta.cashstill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.modelo.*;


public abstract class BaseActualizarCajaAction extends SaveAction {
	
	

    protected void procesarMovimiento(List<DetalleCajaRegistradora> detalles, boolean esEntrada) throws Exception {
    	
    	
    	
        
    	
        for (DetalleCajaRegistradora detalle : detalles) {
            Caja caja = XPersistence.getManager().find(Caja.class, detalle.getCaja().getId());
            if (caja == null) {
                throw new IllegalStateException("No se encontró una Denominacion de billete ingresado en el detalle.");
            }

            int cantidadMovimiento = detalle.getCantidad();
            if (esEntrada) {
                caja.registrarEntrada(cantidadMovimiento);
            } else {
                caja.registrarSalida(cantidadMovimiento);
            }

            if (caja.getValor() == null) {
                throw new IllegalStateException("El valor unitario de la caja no puede ser nulo.");
            }

            caja.setTotal(caja.getValor().multiply(BigDecimal.valueOf(caja.getCantidad())));
            XPersistence.getManager().merge(caja);
        }
        XPersistence.getManager().flush();
       // closeDialog();
    }
    
}
