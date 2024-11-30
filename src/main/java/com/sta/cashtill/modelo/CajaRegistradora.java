package com.sta.cashtill.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import com.sta.cashtill.calculadores.*;

import lombok.*;


@Tab(properties = "movimiento, fechaHora, usuario, totalDetalle, movimientoCajaNombre",
	 editors = "List",
	 defaultOrder = "${fechaHora} asc",
	 rowStyles= { 
				  @RowStyle(style="row-entrada", property="movimiento", value="ENTRADA"),
				  @RowStyle(style="row-salida", property="movimiento", value="SALIDA"),
				  @RowStyle(style="c1-salida", property="movimiento", value="SALIDA"),
				  @RowStyle(style="row-ajuste", property="movimiento", value="AJUSTE"),
				  
				 })

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
abstract public class CajaRegistradora extends Identifiable {
	
	
	@Transient
	@ReadOnly
	public String getMovimientoCajaNombre() {
	    if (this instanceof CajaEntrada) {
	        CajaEntrada cajaEntrada = (CajaEntrada) this;
	        return cajaEntrada.getMovimientoCaja() != null ? cajaEntrada.getMovimientoCaja().getNombre() : null;
	    } else if (this instanceof CajaSalida) {
	        CajaSalida cajaSalida = (CajaSalida) this;
	        return cajaSalida.getMovimientoCaja() != null ? cajaSalida.getMovimientoCaja().getNombre() : null;
	    }
	    return null;
	}
	
	
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
	   	setTotal(getTotalDetalle());
	}
   
	
	@ElementCollection
	@ReadOnly(forViews ="salida")
	@ListProperties(value = "caja.id, cantidad, total")
	@NotNull(message = "El Detalle con los Valores del Movimiento de Caja no puede estar vacio.")
	@Size(min = 1, message = "Debe haber al menos un detalle para realizar el Movimiento de Dinero de la Caja.")
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
	
	@Money
	BigDecimal total;
	
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
   
}
