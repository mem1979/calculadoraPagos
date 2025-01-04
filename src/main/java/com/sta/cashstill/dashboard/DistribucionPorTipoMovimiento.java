package com.sta.cashstill.dashboard;

import java.math.*;

import com.sta.cashstill.enums.*;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class DistribucionPorTipoMovimiento {
	
    private TipoMovimiento tipoMovimiento;
    private BigDecimal total;
  
   
    
}