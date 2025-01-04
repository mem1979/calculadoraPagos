package com.sta.cashstill.auxiliares;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashstill.acciones.*;
import com.sta.cashstill.modelo.*;

import lombok.*;

@Embeddable
@Getter @Setter
public class DetalleCajaRegistradora {
	

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@DescriptionsList(
	    descriptionProperties = "denominacion", 
	       order = "${valor} desc"
	)
     private Caja caja;
	
     
	@Required
	@OnChange(DetalleValorOnChangeAction.class)
	private int cantidad;
	
	@Required
	@Money
	@OnChange(DetalleValorOnChangeAction.class)
 	private BigDecimal total;

	
}
