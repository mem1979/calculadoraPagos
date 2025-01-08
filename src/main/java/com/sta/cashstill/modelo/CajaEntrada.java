package com.sta.cashstill.modelo;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashstill.acciones.*;
import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.validadores.*;

import lombok.*;

@View(members = "categoria, total;" +
				"descripcion;" +
				"documento")

@View(name="entrada",
 	  members = 
 	  "movimientoCaja, importe, conVuelto;" +
 	  "Detalle de INGRESO $ {Caja.NuevoBillete(); detalle;}"+
 	  "Descripcion {documento; descripcion}" )

@Tab(properties = "movimiento, fechaHora, usuario, totalDetalle, movimientoCaja.nombre")

@Entity
@EntityValidator(
	    value = CajaEntradaValidator.class,
	    properties = {
	        @PropertyValue(name="detalle"),  // Se corresponde con getDetalle()
	        @PropertyValue(name="importe"),  // Se corresponde con getImporte()
	        @PropertyValue(name="vuelto"),    // Se corresponde con getVuelto()
	        @PropertyValue(name="conVuelto")    // Se corresponde con getConVuelto()
	        
	       
	    })
@DiscriminatorValue("ENTRADA")
@Getter @Setter
public class CajaEntrada extends CajaRegistradora {

@Required
@LabelFormat(LabelFormatType.SMALL)
@OnChange(ActualizarDescripcionMovimientoCajaOnChange.class)
@ManyToOne(fetch = FetchType.LAZY)
@DescriptionsList(
descriptionProperties = "nombre", 
condition = "${tipoMovimiento} = 'ENTRADA'", 
order = "${nombre} asc")
private MovimientoCaja movimientoCaja; 

}
