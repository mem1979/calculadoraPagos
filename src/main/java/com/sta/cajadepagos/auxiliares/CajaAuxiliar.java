package com.sta.cajadepagos.auxiliares;

import java.math.*;
import java.util.*;

import com.sta.cajadepagos.modelo.*;

import lombok.*;

@Getter
@Setter
//@NoArgsConstructor
@AllArgsConstructor
public class CajaAuxiliar {

    private String denominacion;
    private Integer cantidad;
    private BigDecimal total;
    private Date fechaHora;

    // Constructor adicional para facilitar la creación desde la entidad Caja
    public CajaAuxiliar(Caja caja) {
        this.denominacion = caja.getDenominacion().getNombre(); // Ajusta según tus getters
        this.cantidad = caja.getCantidad();
        this.total = caja.getTotal();
        this.fechaHora = caja.getFechaHora();
    }
}