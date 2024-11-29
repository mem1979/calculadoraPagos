package com.sta.cashtill.modelo;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashtill.auxiliares.*;

import lombok.*;

@View(members = "totalGeneralCaja,movimientoCaja, descripcionMovimientoCaja;" +
        		"descripcion;" +
				"detalle" )
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
private MovimientoCaja movimientoCaja;     

@Label
@Depends("movimientoCaja.id")
@LabelFormat(LabelFormatType.NO_LABEL)
public String getDescripcionMovimientoCaja() {
return movimientoCaja != null && movimientoCaja.getDescripcion() != null ? movimientoCaja.getDescripcion() : "";}



}
