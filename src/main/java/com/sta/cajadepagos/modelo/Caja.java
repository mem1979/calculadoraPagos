package com.sta.cajadepagos.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import com.sta.cajadepagos.acciones.*;
import com.sta.cajadepagos.auxiliares.*;
import com.sta.cajadepagos.calculadores.*;
import com.sta.cajadepagos.validadores.*;

import lombok.*;

@Entity
@Getter
@Setter
@View(members = "cajasAuxiliares,totalCajaCalculado;" +
                "IngresoDeCaja {denominacion, total, cantidad, fechaHora}")

@Tab(properties = "denominacion.nombre, cantidad+, total+, fechaHora",
     editors = "List",
     defaultOrder = "${denominacion.valor} asc")

@Tab(name = "sinNull", 
     properties = "denominacion.nombre, cantidad+, total+, fechaHora",
     defaultOrder = "${denominacion.valor} asc", 
     baseCondition = "${cantidad} != 0 and ${cantidad} is not null")
public class Caja extends Identifiable implements Comparable<Caja> {

	@LabelFormat(LabelFormatType.SMALL)
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

    @Transient
    @SimpleList
 //   @Depends("cantidad, total")
    public List<CajaAuxiliar> getCajasAuxiliares() {
        List<Caja> cajas = XPersistence.getManager()
            .createQuery("SELECT c FROM Caja c ORDER BY c.denominacion.valor ASC", Caja.class)
            .getResultList();

        List<CajaAuxiliar> auxiliares = new ArrayList<>();
        for (Caja caja : cajas) {
            auxiliares.add(new CajaAuxiliar(caja));
        }
        return auxiliares;
    }
    
    @Required
    @DateTime
    @ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fechaHora;

    @Transient
    @Money
    @LargeDisplay(icon = "cash")
    @LabelFormat(LabelFormatType.NO_LABEL)
    @Depends("cantidad, total")
    public BigDecimal getTotalCajaCalculado() {
        BigDecimal total = (BigDecimal) XPersistence.getManager()
            .createQuery("SELECT SUM(c.total) FROM Caja c")
            .getSingleResult();

        return total != null ? total : BigDecimal.ZERO;
    }

    @PrePersist
 //   @PreUpdate
    private void validarDenominacionUnica() {
    	this.fechaHora = new Date(); // Asigna la fecha y hora actual al persistir por primera vez
        Long count = (Long) XPersistence.getManager()
                .createQuery("SELECT COUNT(c) FROM Caja c WHERE c.denominacion = :denominacion AND (c.id != :id OR :id IS NULL)")
                .setParameter("denominacion", this.denominacion)
                .setParameter("id", this.getId())
                .getSingleResult();

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
