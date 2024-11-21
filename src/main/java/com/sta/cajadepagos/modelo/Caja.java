package com.sta.cajadepagos.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.hibernate.envers.*;
import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import com.fasterxml.jackson.annotation.*;
import com.sta.cajadepagos.acciones.*;
import com.sta.cajadepagos.calculadores.*;
import com.sta.cajadepagos.validadores.*;

import lombok.*;


@View(members = "calcularTotalGeneral;" +
"IngresoDeCaja {denominacion, total, cantidad}")

@Tab(properties = "denominacion.nombre, cantidad, total+",
     editors = "List",
     defaultOrder = "${denominacion.valor} asc")

@Tab(name = "sinNull", 
     properties = "denominacion.nombre, cantidad, total+",
     defaultOrder = "${denominacion.valor} asc", 
     baseCondition = "${cantidad} != 0 and ${cantidad} is not null")

@Entity
@Audited
@Getter @Setter
public class Caja extends Identifiable {
	
  
	@NotAudited
	@LabelFormat(LabelFormatType.SMALL)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@DescriptionsList(order = "${valor} asc")
	@DefaultValueCalculator(value = DenominacionPorDefectoCalculator.class)
	private Denominaciones denominacion;

	@NotAudited
    @OnChange(OnChangeCantidadAction.class)
    private Integer cantidad;

    @Money
    @PropertyValidator(value = TotalMultiploValidator.class)
    @OnChange(OnChangeTotalAction.class)
    private BigDecimal total;
    
    // Propiedades para Auditoria de CAJA
    
    @DefaultValueCalculator(value = UsuarioPorDefectoCalculator.class)
    String usuario;
    
    @DefaultValueCalculator(value = CurrentDateCalculator.class)
    Date fechaHora;
    
    String movimintoTipo;
    
    private BigDecimal totalGeneral;

    
    @Money
    @Depends("total,cantidad")
    @LargeDisplay(icon = "cash")
    public BigDecimal getCalcularTotalGeneral() {
        return (BigDecimal) XPersistence.getManager()
            .createQuery("SELECT COALESCE(SUM(c.total), 0) FROM Caja c")
            .getSingleResult();
    }
    
    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registradora_id", nullable = false)
    @JsonIgnore // Evita serialización de la referencia para prevenir ciclos.
    private Registradora registradora;

    @PrePersist
    private void Crear() {
    	setTotalGeneral(getCalcularTotalGeneral());
    	setUsuario(getObtenerNombreUsuario());
    	setMovimintoTipo(getCalcularNombreModulo());
    	setFechaHora(new Date());
  	//validar Denominacion Unica
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
    @PreUpdate
    private void Actualizar() {
   // 	setTotalGeneral(getCalcularTotalGeneral());
    	setUsuario(getObtenerNombreUsuario());
    	setMovimintoTipo(getCalcularNombreModulo());
    	setFechaHora(new Date());
    	
    }
    @PreDelete
    private void Borrar() {
    	setTotalGeneral(getCalcularTotalGeneral());
    	setUsuario(getObtenerNombreUsuario());
    	setMovimintoTipo(getCalcularNombreModulo());
    	setFechaHora(new Date());
    }
    
 // Métodos auxiliares

    private String getObtenerNombreUsuario() {
        // Aquí puedes usar OpenXava o tu propio sistema para obtener el usuario actual.
        // Ejemplo:
        return org.openxava.util.Users.getCurrent() != null ? org.openxava.util.Users.getCurrent() : "No Registrado";
    }

    private String getCalcularNombreModulo() {
        // Si la registradora es null, devuelve "Modificación Caja", de lo contrario "Registro de Pago"
        return this.registradora == null ? "Modificación Caja" : "Registro de Pago";
    }
}

