package com.sta.cashstill.acciones;

import java.math.*;

import org.openxava.actions.*;

import com.sta.cashstill.auxiliares.*;

public class DetalleValorOnChangeAction  extends OnChangePropertyBaseAction {

	 @Override
	    public void execute() throws Exception {
		 
	        DetalleCajaRegistradora detalle = (DetalleCajaRegistradora) getView().getEntity();
	        String propertyChanged = getChangedProperty();

	        // Valores actuales de la vista
	        BigDecimal total = (BigDecimal) getView().getValue("total");
	        Integer cantidad = (Integer) getView().getValue("cantidad");
	        BigDecimal valorDenominacion = detalle.getCaja() != null ? detalle.getCaja().getValor() : null;

	        System.out.println("Propiedad cambiada: " + propertyChanged);
	        

	        // Validar que haya denominación en la caja
	        if (valorDenominacion == null || valorDenominacion.compareTo(BigDecimal.ZERO) <= 0) {
	            addError("La denominación es inválida o el valor es $0.");
	            resetValues();
	            return;
	        }

	        switch (propertyChanged) {
	            case "total":
	                if (total != null ) {
	                    actualizarCantidadDesdeTotal(detalle, total, valorDenominacion);
	                } else {
	                    resetValues();
	                }
	                break;

	            case "cantidad":
	                if (cantidad != null) {
	                    actualizarTotalDesdeCantidad(detalle, cantidad, valorDenominacion);
	                } else {
	                    resetValues();
	                }
	                break;

	            default:
	            	resetValues();
	        }
	    }

	    private void actualizarCantidadDesdeTotal(DetalleCajaRegistradora detalle, BigDecimal total, BigDecimal valorDenominacion) {
	        try {
	            BigDecimal cantidadCalculada = total.divide(valorDenominacion, 0, RoundingMode.DOWN);
	            BigDecimal totalCalculado = cantidadCalculada.multiply(valorDenominacion);

	            if (total.stripTrailingZeros().equals(totalCalculado.stripTrailingZeros())) {
	                getView().setValue("cantidad", cantidadCalculada.intValue());
	            } else {
	                addError("El total ingresado no es un múltiplo válido de la denominación.");
	                resetValues();
	            }
	        } catch (ArithmeticException e) {
	            addError("Error al calcular la cantidad desde el total: " + e.getMessage());
	            resetValues();
	        }
	    }

	    private void actualizarTotalDesdeCantidad(DetalleCajaRegistradora detalle, int cantidad, BigDecimal valorDenominacion) {
	        BigDecimal totalCalculado = valorDenominacion.multiply(BigDecimal.valueOf(cantidad));
	        getView().setValue("total", totalCalculado);
	    }

	    private void resetValues() {
	        getView().setValue("total", BigDecimal.ZERO);
	        getView().setValue("cantidad", 0);
	    }
	}
