package com.sta.cashtill.dashboard;

import java.math.*;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class EvolucionTemporal {
	
    private int year;
    private int month;
    private BigDecimal ingresos;
    private BigDecimal egresos;

    // M�todo auxiliar para convertir el mes y el a�o en un formato legible
    public String getPeriodo() {
        return String.format("%d-%02d", year, month); // Devuelve algo como "2024-01" para enero 2024
    }
}