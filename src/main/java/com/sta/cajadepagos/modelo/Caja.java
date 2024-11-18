package com.sta.cajadepagos.modelo;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import com.sta.cajadepagos.acciones.*;
import com.sta.cajadepagos.calculadores.*;
import com.sta.cajadepagos.validadores.*;

import lombok.*;
@Entity
@Getter
@Setter
@View(members = "denominacion, total, cantidad")

@Tab(properties = "denominacion.nombre,cantidad,total ",
editors ="List",
defaultOrder = "${denominacion.valor} asc")

public class Caja extends Identifiable implements Comparable<Caja> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @DescriptionsList(order = "${valor} asc")
    @DefaultValueCalculator(value = DenominacionPorDefectoCalculator.class)
    private Denominaciones denominacion;

    @OnChange(OnChangeCantidadAction.class)
    private Integer cantidad;

    @Money
    @PropertyValidator(value = TotalMultiploValidator.class)
    @OnChange(OnChangeTotalAction.class)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registradora_id", nullable = true) // Clave foránea a Registradora
    private Registradora registradora;

    @PrePersist
    //@PreUpdate
    private void validarDenominacionUnica() {
        // Crear la consulta JPQL para contar entidades con la misma denominación
        Long count = (Long) XPersistence.getManager()
                .createQuery("SELECT COUNT(c) FROM Caja c WHERE c.denominacion = :denominacion AND (c.id != :id OR :id IS NULL)")
                .setParameter("denominacion", this.denominacion)
                .setParameter("id", this.getId())
                .getSingleResult();

        // Verificar si existe otra entidad con la misma denominación
        if (count > 0) {
            throw new IllegalArgumentException(
                String.format("Ya existe una caja con la denominación '%s'.", this.denominacion.getValor())
            );
        }
    }

    @Override
    public int compareTo(Caja other) {
        if (this.denominacion == null || this.denominacion.getValor() == null) return 1;
        if (other.denominacion == null || other.denominacion.getValor() == null) return -1;
        return other.denominacion.getValor().compareTo(this.denominacion.getValor());
    }
}
