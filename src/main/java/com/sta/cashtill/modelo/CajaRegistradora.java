package com.sta.cashtill.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import com.sta.cashtill.calculadores.*;

import lombok.*;


@Tab(properties = "tipoMovimiento,categoria, fechaHora, usuario, total+",
	 editors = "List",
	 defaultOrder = "${fechaHora} desc",
	 rowStyles= { @RowStyle(style="row-entrada", property="tipoMovimiento", value="ENTRADA"),
				  @RowStyle(style="row-salida", property="tipoMovimiento", value="SALIDA"),
				  @RowStyle(style="c1-salida", property="tipoMovimiento", value="SALIDA"),
				  @RowStyle(style="row-ajuste", property="tipoMovimiento", value="AJUSTE") 
				 })

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
	
	@ElementCollection
	@ListProperties(value = "caja, cantidad, total")
	@ListProperties(forViews ="salida", value = "cajaId, cantidad, total")
	private List<DetalleCajaRegistradora> detalle = new ArrayList<>();
	
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
	            .reduce(BigDecimal.ZERO, BigDecimal::add);}
	@Money
	BigDecimal total;
	
	@Column(length = 10)
	String tipoMovimiento;
	
	@Column(length = 30)
	String categoria;
	
	@PreUpdate @PrePersist
    private void Actualizar() {
	   	setUsuario(getObtenerNombreUsuario());
	   	setFechaHora(new Date());	   
	   	setTotal(getTotalDetalle());
	   	setTipoMovimiento(getMovimiento());
	   	setCategoria(getMovimientoCajaNombre());
	}
	
	
	
	// Métodos auxiliares
    private String getObtenerNombreUsuario() {
    return org.openxava.util.Users.getCurrent() != null ? org.openxava.util.Users.getCurrent() : "No Registrado";}
    
    @Transient
    @ReadOnly
 // Obtiene el tipo de Movimiento segun Dtype
    public String getMovimiento(){
        // Retorna el valor de discriminador directamente
        return this.getClass().getAnnotation(DiscriminatorValue.class) != null
               ? this.getClass().getAnnotation(DiscriminatorValue.class).value() : null;
    }
   
    @Transient
	@ReadOnly
	// Obtiene la Categoria de movimientoCaja
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
    
}
