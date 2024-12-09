package com.sta.cashtill.dashboard;

import java.math.*;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class ResumenPorCategoria {
    private String categoria;
    private BigDecimal total;
}