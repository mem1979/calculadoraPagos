package com.sta.cashtill.dashboard;

import java.math.*;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class ResumenPorUsuario {
    private String usuario;
    private BigDecimal total;
}