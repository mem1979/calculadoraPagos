package com.sta.cashtill.auxiliares;

import java.math.*;

import javax.persistence.*;

import lombok.*;

@Embeddable
@Getter @Setter

public class HistoricoDeCaja {
	
	 private String id;
	 private int cantidad;
	 private BigDecimal total;

}
