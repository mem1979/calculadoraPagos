package com.sta.cashtill.auxiliares;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import lombok.*;

@View(members= "codigoCalculado; valor")

@Tab(properties = "nombre",
editors ="List",
defaultOrder = "${valor} asc")

@Entity
@Getter @Setter
public class Denominaciones extends Identifiable {

    @ReadOnly
    private String nombre;

    @Money
    @Required
    @DefaultValueCalculator(ZeroBigDecimalCalculator.class)
    private BigDecimal valor;

    /**
     * Código calculado dinámicamente en función del valor.
     * Si valor es null, devuelve un valor predeterminado.
     */
    @Depends("valor")
    public String getCodigoCalculado() {
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

   /**
     * Actualiza el código antes de persistir o actualizar.
     */
    @PrePersist
    @PreUpdate
    private void validarNombreUnicoYGuardar() {
        // Actualiza el nombre basado en el código calculado antes de validar
        setNombre(getCodigoCalculado());

        // Consulta para verificar si ya existe un registro con el mismo nombre
        long count = XPersistence.getManager()
            .createQuery("SELECT COUNT(d) FROM Denominaciones d WHERE d.nombre = :nombre AND d.id != :id", Long.class)
            .setParameter("nombre", this.nombre)
            .setParameter("id", this.getId() == null ? "" : this.getId())
            .getSingleResult();

        // Si existe un registro con el mismo nombre, lanzar excepción
        if (count > 0) {
            throw new IllegalStateException("La Denominacion '" + nombre + "' ya está en uso. Por favor, Agrega un valor diferente.");
        }
    }
}