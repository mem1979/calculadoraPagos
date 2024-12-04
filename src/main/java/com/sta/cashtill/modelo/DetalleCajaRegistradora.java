package com.sta.cashtill.modelo;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;

import com.sta.cashtill.acciones.*;

import lombok.*;


@Embeddable
@Getter @Setter
public class DetalleCajaRegistradora {
	
	
	@OnChange(DetalleTotalOnChangeAction.class)
	@ManyToOne(fetch = FetchType.LAZY, optional = false) // 1
	@DescriptionsList(descriptionProperties="id",
	  				  order="${denominacion.valor} asc")
	  				//  condition= "${cantidad} != 0")
	private Caja caja;
	
		
	@Required
	@OnChange(DetalleCantidadOnChangeAction.class)
	@DefaultValueCalculator(ZeroIntegerCalculator.class)
	private int cantidad;

	
	@Required
	@Money
	@OnChange(DetalleTotalOnChangeAction.class)
	@DefaultValueCalculator(ZeroBigDecimalCalculator.class)
	private BigDecimal total;
}
