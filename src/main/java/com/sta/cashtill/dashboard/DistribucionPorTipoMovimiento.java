package com.sta.cashtill.dashboard;

import java.math.*;

import com.sta.cashtill.enums.*;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class DistribucionPorTipoMovimiento {
	
    private TipoMovimiento tipoMovimiento;
    private BigDecimal total;
  
   
    
}