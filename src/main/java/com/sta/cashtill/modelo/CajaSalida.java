package com.sta.cashtill.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;

import com.sta.cashtill.acciones.*;
import com.sta.cashtill.auxiliares.*;
import com.sta.cashtill.enums.*;

import lombok.*;

@View(name="salida",
members = 
		 "movimientoCaja, importe;" +
		 "Detalle de SALIDA $ {estrategiaPagos; detalle, cajaOrdenada}" +
		 "Descripcion {documento; descripcion }")

@View( members = "categoria, total;" +
				 "descripcion; documento")

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
	@OnChange(CajaSalidaLimpiarColeccionAlCambiarEstrategiaAction.class)
	@Action(value="MovimientoCaja.CalcularSalida$")
    private EstrategiaPagos estrategiaPagos;
		
    @Money
    @Required
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(value = ZeroBigDecimalCalculator.class)
    private BigDecimal importe;
    
    @SuppressWarnings("unchecked")
    @ReadOnly 
    @SimpleList
    @ListProperties ("id, cantidad, total+")
    public Collection<Caja> getCajaOrdenada() {
        Query query = XPersistence.getManager().createQuery("FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC");
        return query.getResultList();
    }
    
    
 }
