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
	  members = "movimientoCaja, descripcionMovimientoCaja;" +
			    "estrategiaPagos;" +
				"importePagos, cantidadPagos, montoTotalPagos;" +
			  	"descripcion;" +
			  	"detalle")

@View(members = "movimientoCaja, descripcionMovimientoCaja;" +
	  			"descripcion;" +
	  			"detalle;" +
	  			"estrategiaPagos;" +
				"importePagos, cantidadPagos, montoTotalPagos")

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
	
	@Label
	@Depends("movimientoCaja.id")
	@LabelFormat(LabelFormatType.NO_LABEL)
	public String getDescripcionMovimientoCaja() {
	return movimientoCaja != null && movimientoCaja.getDescripcion() != null ? movimientoCaja.getDescripcion() : "";}
	
	@Required
	@Action(value="MovimientoCaja.CalcularSalida$", alwaysEnabled=true)
    @Enumerated(EnumType.STRING)
    @LabelFormat(LabelFormatType.SMALL)
    private EstrategiaPagos estrategiaPagos;
	
	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(value = ZeroIntegerCalculator.class)
    private int cantidadPagos;

    @Money
    @Required
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(value = ZeroBigDecimalCalculator.class)
    private BigDecimal importePagos;
    
    @Money
    @LabelFormat(LabelFormatType.SMALL)
    @Depends("importePagos, cantidadPagos")
    public BigDecimal getMontoTotalPagos() {
        if (importePagos == null || cantidadPagos <= 0) {
            return BigDecimal.ZERO;
        }
        	return importePagos.multiply(BigDecimal.valueOf(cantidadPagos));
    }
    
    
 }
