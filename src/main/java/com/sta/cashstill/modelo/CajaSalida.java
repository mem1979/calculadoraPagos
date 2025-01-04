package com.sta.cashstill.modelo;

import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;

import com.sta.cashstill.acciones.*;
import com.sta.cashstill.auxiliares.*;

import lombok.*;

@View(name="salida",
members = 
		 "movimientoCaja, importe;" +
		 "Detalle de SALIDA $ {MovimientoCaja.CalcularSalida$(), detalle, cajaOrdenada}" +
		 "Descripcion {documento; descripcion }")

@View(members = "categoria, total;" +
				 "descripcion; documento")

@Entity
@EntityValidator(
	    value = com.sta.cashstill.validadores.CajaSalidaValidator.class,
	    properties = {
		        @PropertyValue(name="detalle"),  // Se corresponde con getDetalle()
		        @PropertyValue(name="importe"),  // Se corresponde con getImporte()
		        @PropertyValue(name="totalDetalle"),   
		    })
	
@DiscriminatorValue("SALIDA")
@Getter @Setter
public class CajaSalida extends CajaRegistradora {
	
	@Required
	@ReferenceView("Simple")
	@LabelFormat(LabelFormatType.SMALL)
	@OnChange(ActualizarDescripcionMovimientoCajaOnChange.class)
	@ManyToOne(fetch = FetchType.LAZY)
	@DescriptionsList(
	    descriptionProperties = "nombre", // Muestra el campo 'nombre' de MovimientoCaja
	    condition = "${tipoMovimiento} = 'SALIDA' AND ${nombre} <> 'VUELTO'", // Excluye 'VUELTO'
	    order = "${nombre} asc"           // Ordena la lista por 'nombre'
	)
	private MovimientoCaja movimientoCaja;
	
	
		
    @SuppressWarnings("unchecked")
    @ReadOnly 
    @SimpleList
    @ListProperties ("denominacion, cantidad, total+")
    public Collection<Caja> getCajaOrdenada() {
        Query query = XPersistence.getManager().createQuery("FROM Caja c WHERE c.cantidad > 0 ORDER BY c.valor DESC");
        return query.getResultList();
    }
    
    
 }
