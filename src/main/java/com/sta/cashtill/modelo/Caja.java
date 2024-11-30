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

@Tab(properties = "denominacion.nombre, cantidad, total+",
     editors = "List",
     defaultOrder = "${denominacion.valor} asc")

@Tab(name = "sinNull", 
     properties = "denominacion.nombre, cantidad, total+",
     defaultOrder = "${denominacion.valor} asc", 
     baseCondition = "${cantidad} != 0 and ${cantidad} is not null")

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
        // Guarda el Total Calculado
        if (getCalculaTotal() == null || getCalculaTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El Total del Moviminiento de Caja no puede ser $0");
        } else {
        	setTotal(getCalculaTotal());}
        if (denominacion != null && denominacion.getNombre() != null) {
            setId(getDenominacion().getNombre());}
    }
    
    @PreUpdate
    private void alActualizar() throws Exception {
    	if (getCalculaTotal() == null || getCalculaTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El Total del Moviminiento de Caja no puede ser $0.-");
        } else {
        }   	setTotal(getCalculaTotal());}

    // Métodos auxiliares
    private void validarDenominacionUnica() {
        Long count = (Long) XPersistence.getManager()
            .createQuery("SELECT COUNT(c) FROM Caja c WHERE c.denominacion = :denominacion AND (c.id != :id OR :id IS NULL)")
            .setParameter("denominacion", this.denominacion)
            .setParameter("id", this.getId())
            .getSingleResult();

        if (count > 0) {
            throw new javax.validation.ValidationException(
                XavaResources.getString("denominacion_duplicada", denominacion.getValor().toPlainString())
            );
        }
    }
}
 
 