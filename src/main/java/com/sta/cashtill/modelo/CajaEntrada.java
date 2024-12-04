package com.sta.cashtill.modelo;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashtill.acciones.*;
import com.sta.cashtill.auxiliares.*;

import lombok.*;

@View(members = "categoria, total;" +
				"descripcion;" +
				"detalle,documento")

@View(name="entrada",
 	  members = "movimientoCaja;" +
 			  	"descripcion;" +
  		   		"detalle,documento")

@Tab(properties = "movimiento, fechaHora, usuario, totalDetalle, movimientoCaja.nombre")

@Entity
@DiscriminatorValue("ENTRADA")
@Getter @Setter
public class CajaEntrada extends CajaRegistradora {

@Required
@LabelFormat(LabelFormatType.SMALL)
@ManyToOne(fetch = FetchType.LAZY)
@DescriptionsList(
descriptionProperties = "nombre", 
condition = "${tipoMovimiento} = 'ENTRADA'", 
order = "${nombre} asc")
@OnChange(CajaRegistradoraActualizarDetalleCategoriaMovimientoAction.class)
private MovimientoCaja movimientoCaja; 

@LabelFormat(LabelFormatType.SMALL)
@File(maxFileSizeInKb=90)
@Column(length=32)
private String documento;

}
