package com.sta.cashtill.acciones;

import java.math.*;

import org.openxava.actions.*;

import com.sta.cashtill.modelo.*;

public class DetalleTotalOnChangeAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        DetalleCajaRegistradora detalle = (DetalleCajaRegistradora) getView().getEntity();
        BigDecimal total = (BigDecimal) getView().getValue("total");

        System.out.println("Inicio de la acci�n");
        System.out.println("Nuevo valor ingresado para 'total': " + total);

        if (detalle.getCaja() != null && detalle.getCaja().getDenominacion() != null) {
            BigDecimal valorDenominacion = detalle.getCaja().getDenominacion().getValor();
            System.out.println("Caja encontrada con denominaci�n: " + valorDenominacion);

            if (total != null && valorDenominacion.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("Validando total y denominaci�n...");
                
                BigDecimal cantidadCalculada = total.divide(valorDenominacion, 0, RoundingMode.DOWN);
                BigDecimal totalCalculado = cantidadCalculada.multiply(valorDenominacion);

                System.out.println("Cantidad calculada: " + cantidadCalculada);
                System.out.println("Total calculado a partir de la cantidad: " + totalCalculado);

                if (total.stripTrailingZeros().equals(totalCalculado.stripTrailingZeros())) {
                    System.out.println("El total es m�ltiplo v�lido de la denominaci�n.");
                    int cantidad = cantidadCalculada.intValue();
                    detalle.setCantidad(cantidad);
                    getView().setValue("cantidad", cantidad);
                } else {
                    System.out.println("Error: El total no es un m�ltiplo v�lido de la denominaci�n.");
                    addError("El total ingresado no es un m�ltiplo v�lido de la denominaci�n.");
                    getView().setValue("total", BigDecimal.ZERO);
                    getView().setValue("cantidad", 0);
                }
            } else {
                System.out.println("Error: Total es nulo o denominaci�n inv�lida.");
                addError("El total no puede ser procesado debido a un valor inv�lido.");
                getView().setValue("total", BigDecimal.ZERO);
            }
        } 
        
        System.out.println("Fin de la acci�n");
    }
}
