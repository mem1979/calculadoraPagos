package com.sta.cashtill.modelo;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.util.*;

import com.sta.cashtill.auxiliares.*;
import com.sta.cashtill.calculadores.*;

import lombok.*;

@View(members="id; denominacion")

@View(name="salida", members="id")

@Tab(properties = "denominacion.nombre, cantidad, total+",
     defaultOrder = "${denominacion.valor} asc", 
     editors = "List")

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"denominacion_id"}))
@Entity
@Getter @Setter
public class Caja {

    @Id
    @SearchKey
    @ReadOnly
    private String id;
    
 
    @LabelFormat(LabelFormatType.SMALL)
    @DescriptionsList(order = "${valor} asc")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @DefaultValueCalculator(value = DenominacionPorDefectoCalculator.class)
    private Denominaciones denominacion;

    @DefaultValueCalculator(ZeroIntegerCalculator.class)
    private int cantidad;

    @Money
    private BigDecimal total;

    @Money
    @Depends("cantidad, denominacion.valor")
    public BigDecimal getCalculaTotal() {
        if (denominacion != null && cantidad != 0) {
            return denominacion.getValor().multiply(BigDecimal.valueOf(cantidad));
        } else {
            return BigDecimal.ZERO;
        }
    }
    
    @PrePersist
    private void alCrear() throws Exception {
        validarDenominacionUnica();
        if (denominacion != null && denominacion.getNombre() != null) {
            setId(getDenominacion().getNombre());}
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
            .createQuery("SELECT COUNT(c) FROM Caja c WHERE c.denominacion = :denominacion AND (c.id != :id OR :id IS NULL)")
            .setParameter("denominacion", this.denominacion)
            .setParameter("id", this.getId())
            .getSingleResult();

        if (count > 0) {
            throw new javax.validation.ValidationException(
                XavaResources.getString("Denominacion Duplicada", denominacion.getValor().toPlainString())
            );
        }
    }
}
 
 