package com.sta.cashtill.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import com.sta.cashtill.calculadores.*;

import lombok.*;


@Tab(properties = "movimiento, fechaHora, usuario, totalDetalle",
	 editors = "List",
	 defaultOrder = "${fechaHora} asc")

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
abstract public class CajaRegistradora extends Identifiable {
	
	
    @DateTime
    @ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fechaHora;
	
	@ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(UsuarioPorDefectoCalculator.class)
	String usuario;
	
	@TextArea
	String descripcion;
	
	@PreUpdate @PrePersist
    private void Actualizar() {
	   	setUsuario(getObtenerNombreUsuario());
	   	setFechaHora(new Date());	   
   	}
   
	@ElementCollection
	@ListProperties(value = "caja.id, cantidad, total")
	Collection<DetalleCajaRegistradora> detalle;
	
	// Propiedad calculada para sumar el total de los detalles
	@Money
	@ReadOnly
	public BigDecimal getTotalDetalle() {
	    if (detalle == null || detalle.isEmpty()) {
	        return BigDecimal.ZERO;
	    }

	    // Sumar los valores de 'total' en la colección 'detalle'
	    return detalle.stream()
	            .map(d -> d.getTotal() != null ? d.getTotal() : BigDecimal.ZERO)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
 // Métodos auxiliares
    private String getObtenerNombreUsuario() {
    return org.openxava.util.Users.getCurrent() != null ? org.openxava.util.Users.getCurrent() : "No Registrado";}
    
    @Transient
    @ReadOnly
    public String getMovimiento(){
        // Retorna el valor de discriminador directamente
        return this.getClass().getAnnotation(DiscriminatorValue.class) != null
                ? this.getClass().getAnnotation(DiscriminatorValue.class).value() : null;
    }
    
    // Propiedad calculada con consulta JPQL
    @Money
    @LargeDisplay(icon="cash-multiple")
    @LabelFormat(LabelFormatType.NO_LABEL)
    @Transient
    @DefaultValueCalculator(TotalCajaCalculador.class)
    BigDecimal totalGeneralCaja;
       
   
}
