package com.sta.cashtill.modelo;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;

import com.sta.cashtill.acciones.*;
import com.sta.cashtill.auxiliares.*;
import com.sta.cashtill.enums.*;

import lombok.*;

@View(name="salida",
	  members = "movimientoCaja;" +
			    "estrategiaPagos, importe;" +
				"descripcion;" +
			  	"detalle")

@View( members = "categoria, total;" +
				 "descripcion;" +
	  			 "detalle")

@Entity
@DiscriminatorValue("SALIDA")
@Getter @Setter
public class CajaSalida extends CajaRegistradora {
	
	@Required
	@ReferenceView("Simple")
	@LabelFormat(LabelFormatType.SMALL)
	@ManyToOne(fetch = FetchType.LAZY)
	@DescriptionsList(
	    descriptionProperties = "nombre", // Muestra el campo 'nombre' de MovimientoCaja
	    condition = "${tipoMovimiento} = 'SALIDA'", // Usa comillas simples para el valor de filtro
	    order = "${nombre} asc"           // Ordena la lista por 'nombre'
	)
	@OnChange(CajaRegistradoraActualizarDetalleCategoriaMovimientoAction.class)
	private MovimientoCaja movimientoCaja;
	
	@Required
	@Enumerated(EnumType.STRING)
    @LabelFormat(LabelFormatType.SMALL)
    private EstrategiaPagos estrategiaPagos;
		
    @Money
    @Required
    @Action(value="MovimientoCaja.CalcularSalida$")
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(value = ZeroBigDecimalCalculator.class)
    private BigDecimal importe;
    
 
    
    
 }
