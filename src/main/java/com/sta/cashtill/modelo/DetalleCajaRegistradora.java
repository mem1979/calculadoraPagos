package com.sta.cashtill.modelo;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;

import com.sta.cashtill.acciones.*;

import lombok.*;

@Embeddable
@Getter @Setter
public class DetalleCajaRegistradora {
	
    
   // @OnChange(CajaIdOnChangeAction.class)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @DescriptionsList(descriptionProperties = "id",
                      order = "${denominacion.valor} desc")
    private Caja caja;
	
	@Required
	@OnChange(DetalleCantidadOnChangeAction.class)
	private int cantidad;

	@Required
	@Money
	@OnChange(DetalleTotalOnChangeAction.class)
 	private BigDecimal total;
}
