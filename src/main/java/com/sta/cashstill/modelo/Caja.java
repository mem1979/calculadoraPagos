package com.sta.cashstill.modelo;

import java.math.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.openxava.util.*;

import lombok.*;



@View(members="denominacionCalculada; valor")

@Tab(properties = "denominacion, cantidad, total+",
     defaultOrder = "${valor} asc", 
     editors = "List")


@Entity
@Getter @Setter
public class Caja extends Identifiable{

   
	@SearchKey
    @ReadOnly
    private String denominacion;
    
    /**
     * id calculado dinámicamente en función del valor.
     * Si valor es null, devuelve un valor predeterminado.
     */
    
    @Depends("valor")
    @ReadOnly
    public String getDenominacionCalculada() {
    	if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
    	    return "No definido";
    	}

        if (valor.compareTo(BigDecimal.valueOf(1000)) < 0) {
            // Si el valor es menor a 1000, mostrar directamente el valor con formato
            return "$" + valor.stripTrailingZeros().toPlainString() + ".-";
        } else if (valor.compareTo(BigDecimal.valueOf(1000000)) < 0) {
            // Si el valor es menor a 1,000,000, mostrar como "Mil"
            BigDecimal valorEnMiles = valor.divide(BigDecimal.valueOf(1000));
            return "$" + valorEnMiles.stripTrailingZeros().toPlainString() + "Mil.-";
        } else {
            // Si el valor es igual o mayor a 1,000,000, mostrar como "Millones"
            BigDecimal valorEnMillones = valor.divide(BigDecimal.valueOf(1000000));
            return "$" + valorEnMillones.stripTrailingZeros().toPlainString() + "Millones.-";
        }
    }
    
 //   @Money
    @Required
    @DefaultValueCalculator(ZeroBigDecimalCalculator.class)
    @Digits(integer = 10, fraction = 0) // Hasta 10 dígitos enteros y 0 decimales
    private BigDecimal valor;

    @DefaultValueCalculator(ZeroIntegerCalculator.class)
    private int cantidad;

    @Money
    private BigDecimal total;

    @Money
    @Depends("cantidad, valor")
    public BigDecimal getCalculaTotal() {
        if (valor.compareTo(BigDecimal.ZERO) < 0 && cantidad != 0) {
            return valor.multiply(BigDecimal.valueOf(cantidad));
        } else {
            return BigDecimal.ZERO;
        }
    }
    
    @PrePersist
    private void alCrear() throws Exception {
        validarDenominacionUnica();
        setDenominacion(getDenominacionCalculada());
    }
    
    @PreUpdate
    private void alActualizar() throws Exception {
        // Validar si el cálculo total es nulo o menor que cero
        if (getCalculaTotal() == null || getCalculaTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El Total del Movimiento de Caja no puede ser $0.-");
        }

        // Si la validación es exitosa, actualizar el total
        setTotal(getCalculaTotal());
    }
    
 // Métodos auxiliares
    private void validarDenominacionUnica() {
        Long count = (Long) XPersistence.getManager()
            .createQuery("SELECT COUNT(c) FROM Caja c WHERE c.denominacion = :denominacion")
            .setParameter("denominacion", getDenominacionCalculada())
            .getSingleResult();

        if (count > 0) {
        	 throw new javax.validation.ValidationException(
        	            XavaResources.getString("denominacion_duplicada", getDenominacionCalculada())
            );
        }
    }
    
   
}

 
 