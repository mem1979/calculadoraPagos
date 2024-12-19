package com.sta.cashtill.modelo;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashtill.acciones.*;
import com.sta.cashtill.auxiliares.*;

import lombok.*;

@View(members = "categoria, total;" +
				"descripcion;" +
				"documento")

@View(name="entrada",
 	  members = 
 	  "movimientoCaja;" +
 	  "Detalle de INGRESO $ {Caja.NuevoBillete(); detalle}"+
 	  "Descripcion {documento; descripcion }" )

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

}
