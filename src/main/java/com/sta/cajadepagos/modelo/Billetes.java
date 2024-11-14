package com.sta.cajadepagos.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import lombok.*;

@View(members = 
"Cantidad de Billetes [ " +
    "cantidadCinco; cantidadDies; cantidadVeinte; cantidadCincuenta; cantidadCien; " +
    "cantidadDosientos; cantidadQuinientos; cantidadMil; cantidadDosMil; cantidadDiesMil; cantidadVeinteMil ;" +
"] " +
"Detalles del Pago [ " +
    "totalBilletesEnPesos;" +
"montoPago, cantidadPagos, totalApagar; distribucionBilletes; " +
"]"
)

@Entity
@Getter @Setter
public class Billetes extends Identifiable {

    // Definición de cantidades para cada denominación de billete
	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadCinco;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadDies;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadVeinte;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadCincuenta;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadCien;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadDosientos;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadQuinientos;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadMil;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadDosMil;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadDiesMil;

	@LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadVeinteMil;

    // Propiedad calculada para obtener el total en pesos de todos los billetes
    @Depends("cantidadVeinteMil, cantidadDiesMil, cantidadDosMil, cantidadMil, cantidadQuinientos, cantidadDosientos, cantidadCien, cantidadCincuenta, cantidadVeinte, cantidadDies, cantidadCinco")
    @Money
    public BigDecimal getTotalBilletesEnPesos() {
        BigDecimal total = BigDecimal.ZERO;

        total = total.add(BigDecimal.valueOf(cantidadVeinteMil).multiply(BigDecimal.valueOf(20000)));
        total = total.add(BigDecimal.valueOf(cantidadDiesMil).multiply(BigDecimal.valueOf(10000)));
        total = total.add(BigDecimal.valueOf(cantidadDosMil).multiply(BigDecimal.valueOf(2000)));
        total = total.add(BigDecimal.valueOf(cantidadMil).multiply(BigDecimal.valueOf(1000)));
        total = total.add(BigDecimal.valueOf(cantidadQuinientos).multiply(BigDecimal.valueOf(500)));
        total = total.add(BigDecimal.valueOf(cantidadDosientos).multiply(BigDecimal.valueOf(200)));
        total = total.add(BigDecimal.valueOf(cantidadCien).multiply(BigDecimal.valueOf(100)));
        total = total.add(BigDecimal.valueOf(cantidadCincuenta).multiply(BigDecimal.valueOf(50)));
        total = total.add(BigDecimal.valueOf(cantidadVeinte).multiply(BigDecimal.valueOf(20)));
        total = total.add(BigDecimal.valueOf(cantidadDies).multiply(BigDecimal.valueOf(10)));
        total = total.add(BigDecimal.valueOf(cantidadCinco).multiply(BigDecimal.valueOf(5)));

        return total;
    }

    @DefaultValueCalculator(IntegerCalculator.class)
    int cantidadPagos;

    @Money
    @DefaultValueCalculator(BigDecimalCalculator.class)
    BigDecimal montoPago;

    @Depends("montoPago, cantidadPagos")
    @Money
    public BigDecimal getTotalApagar() {
        return montoPago.multiply(BigDecimal.valueOf(cantidadPagos));
    }

    // Propiedad calculada para la distribución de billetes en formato de cadena
    @Depends("cantidadVeinteMil, cantidadDiesMil, cantidadDosMil, cantidadMil, cantidadQuinientos, cantidadDosientos, cantidadCien, cantidadCincuenta, cantidadVeinte, cantidadDies, cantidadCinco, montoPago, cantidadPagos")
    @TextArea
    public String getDistribucionBilletes() {
        BigDecimal montoTotal = montoPago.multiply(BigDecimal.valueOf(cantidadPagos));
        Map<String, BigDecimal> valoresBilletes = Map.ofEntries(
            Map.entry("20000", BigDecimal.valueOf(20000)),
            Map.entry("10000", BigDecimal.valueOf(10000)),
            Map.entry("2000", BigDecimal.valueOf(2000)),
            Map.entry("1000", BigDecimal.valueOf(1000)),
            Map.entry("500", BigDecimal.valueOf(500)),
            Map.entry("200", BigDecimal.valueOf(200)),
            Map.entry("100", BigDecimal.valueOf(100)),
            Map.entry("50", BigDecimal.valueOf(50)),
            Map.entry("20", BigDecimal.valueOf(20)),
            Map.entry("10", BigDecimal.valueOf(10)),
            Map.entry("5", BigDecimal.valueOf(5))
        );

        Map<String, Integer> billetesDistribucion = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : valoresBilletes.entrySet()) {
            String tipoBillete = entry.getKey();
            BigDecimal valorBillete = entry.getValue();
            int cantidadBillete = montoTotal.divide(valorBillete, BigDecimal.ROUND_DOWN).intValue();
            billetesDistribucion.put(tipoBillete, cantidadBillete);
            montoTotal = montoTotal.remainder(valorBillete);
        }

        // Construcción del resultado en formato de cadena
        StringBuilder resultado = new StringBuilder("Cada Pago se debe preparar con un Fajo que contenga:\n");

        for (Map.Entry<String, Integer> entry : billetesDistribucion.entrySet()) {
            int cantidad = entry.getValue();
            if (cantidad > 0) { // Solo incluir billetes con cantidad mayor a 0
                resultado.append("• ")
                         .append(cantidad)
                         .append(" Billete")
                         .append(cantidad > 1 ? "s" : "") // Singular o plural
                         .append(" de $")
                         .append(entry.getKey())
                         .append(".-\n");
            }
        }

        // Actualización de propiedades de la clase con los valores calculados
        cantidadVeinteMil = billetesDistribucion.getOrDefault("20000", 0);
        cantidadDiesMil = billetesDistribucion.getOrDefault("10000", 0);
        cantidadDosMil = billetesDistribucion.getOrDefault("2000", 0);
        cantidadMil = billetesDistribucion.getOrDefault("1000", 0);
        cantidadQuinientos = billetesDistribucion.getOrDefault("500", 0);
        cantidadDosientos = billetesDistribucion.getOrDefault("200", 0);
        cantidadCien = billetesDistribucion.getOrDefault("100", 0);
        cantidadCincuenta = billetesDistribucion.getOrDefault("50", 0);
        cantidadVeinte = billetesDistribucion.getOrDefault("20", 0);
        cantidadDies = billetesDistribucion.getOrDefault("10", 0);
        cantidadCinco = billetesDistribucion.getOrDefault("5", 0);

        return resultado.toString();
    }
}
