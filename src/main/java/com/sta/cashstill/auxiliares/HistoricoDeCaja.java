package com.sta.cashstill.auxiliares;

import java.math.*;

import javax.persistence.*;

import org.openxava.annotations.*;

import lombok.*;

@Embeddable
@Getter @Setter

public class HistoricoDeCaja {
	
	 private String denominacion;
	 
	 private int cantidad;
	 
	 @Money
	 private BigDecimal total;

}
