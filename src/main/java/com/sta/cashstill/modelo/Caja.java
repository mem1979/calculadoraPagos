package com.sta.cashstill.modelo;

import java.math.*;

import javax.persistence.*;
import javax.validation.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.openxava.util.*;

import lombok.*;

/**
 * Representa una 'Caja' con un valor unitario, una cantidad y un total calculado.
 */
@Entity
@Tab(
    properties = "denominacion, cantidad, total+",
    defaultOrder = "${valor} asc",
    editors = "List"
)
@View(members = "denominacionCalculada; valor")
@Getter @Setter
public class Caja extends Identifiable {
    
   
    @ReadOnly
    @DisplaySize(10)
    private String denominacion;
    
    /**
     * Valor unitario de la Caja.
     */
    @Money
    @DisplaySize(10)
    @DefaultValueCalculator(ZeroBigDecimalCalculator.class)
    private BigDecimal valor;

    /**
     * Cantidad de unidades.
     */
    @DefaultValueCalculator(ZeroIntegerCalculator.class)
    private int cantidad;

    /**
     * Total monetario calculado a partir de valor x cantidad (o seg�n l�gica interna).
     */
    @Money
    private BigDecimal total;

    /**
     * Denominaci�n calculada en funci�n del valor.
     * Se formatea de diferentes maneras dependiendo del monto.
     */
    @Depends("valor")
    @ReadOnly
    @DisplaySize(15)
    public String getDenominacionCalculada() {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            return "No definido";
        }

        if (valor.compareTo(BigDecimal.valueOf(1000)) < 0) {
            // Si el valor es menor a 1000, mostrar directamente el valor con formato
            return "$" + valor.stripTrailingZeros().toPlainString() + ".-";
        } 
        else if (valor.compareTo(BigDecimal.valueOf(1000000)) < 0) {
            // Si el valor es menor a 1,000,000, mostrar como "Mil"
            BigDecimal valorEnMiles = valor.divide(BigDecimal.valueOf(1000));
            return "$" + valorEnMiles.stripTrailingZeros().toPlainString() + "Mil.-";
        } 
        else {
            // Si el valor es igual o mayor a 1,000,000, mostrar como "Millones"
            BigDecimal valorEnMillones = valor.divide(BigDecimal.valueOf(1000000));
            return "$" + valorEnMillones.stripTrailingZeros().toPlainString() + "Millones.-";
        }
    }

    /**
     * C�lculo interno para el Total. 
     */
    @Money
    @Depends("cantidad, valor")
    public BigDecimal getCalculaTotal() {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        // Ajustar la l�gica seg�n tus necesidades:
        return valor.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Se ejecuta antes de persistir una nueva entidad.
     * Valida la denominaci�n y la asigna.
     */
    @PrePersist
    private void alCrear() throws Exception {
        validarDenominacionUnica();
        setDenominacion(getDenominacionCalculada());
        setTotal(getCalculaTotal());
    }

    /**
     * Se ejecuta antes de actualizar una entidad existente.
     * Valida el total y lo recalcula.
     */
    @PreUpdate
    private void alActualizar() throws Exception {
        if (getCalculaTotal() == null || getCalculaTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El Total del Movimiento de Caja no puede ser $0.- o negativo.");
        }
        setTotal(getCalculaTotal());
     //   setDenominacion(getDenominacionCalculada()); 
    }

    // ---------------- M�todos auxiliares ----------------

    /**
     * Valida que la denominaci�n calculada no exista previamente en la BD.
     */
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
    
    // ---------------- M�todos para actualizar la caja desde otras clases/acciones ----------------

    /**
     * Registra una entrada en la Caja, proveniente por ejemplo de una acci�n o de la clase 'CajaEntrada'.
     * @param cantidadEntrada Cantidad de unidades que ingresan a la caja
     * @throws ValidationException Si la cantidad es inv�lida
     */
    public void registrarEntrada(int cantidadEntrada) {
        if (cantidadEntrada <= 0) {
            throw new ValidationException("La cantidad a ingresar debe ser mayor a cero.");
        }
        this.cantidad += cantidadEntrada;
        recalcularTotales();
    }

    /**
     * Registra una salida en la Caja, proveniente por ejemplo de una acci�n o de la clase 'CajaSalida'.
     * @param cantidadSalida Cantidad de unidades que salen de la caja
     * @throws ValidationException Si la cantidad es inv�lida o supera el disponible
     */
    public void registrarSalida(int cantidadSalida) {
        if (cantidadSalida <= 0) {
            throw new ValidationException("La cantidad a retirar debe ser mayor a cero.");
        }
        if (this.cantidad < cantidadSalida) {
            throw new ValidationException("No hay suficientes billetes de " + denominacion +" en caja para la salida solicitada.");
        }
        this.cantidad -= cantidadSalida;
        recalcularTotales();
    }

    /**
     * M�todo para recalcular y actualizar el total y la denominaci�n,
     * �til cuando se modifican la cantidad y/o el valor desde otras clases.
     */
    public void recalcularTotales() {
        setTotal(getCalculaTotal());
     //   setDenominacion(getDenominacionCalculada());
    }

}


 
 