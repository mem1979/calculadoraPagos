package com.sta.cajadepagos.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.hibernate.envers.*;
import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import com.sta.cajadepagos.enums.*;

import lombok.*;

@View(members = "caja," +
        "fechaHora;" +
       "totalSaldo;" +
        "totalCantidad;" +
        "RegistroDePagos {;" +
        "estrategiaPagos;" +
        "importePagos, cantidadPagos, montoTotalPagos; " +
        "detallePagos;" +
        "}"
)
@Tab(properties = "fechaHora, importePagos, cantidadPagos, montoTotalPagos",
        editors = "List, Charts",
        defaultOrder = "${fechaHora} desc")
@Entity
@Getter
@Setter
public class Registradora extends Identifiable {

    @Required
    @DateTime
    @ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fechaHora;

    @LabelFormat(LabelFormatType.SMALL)
    private int cantidadPagos;

    @Money
    @LabelFormat(LabelFormatType.SMALL)
    private BigDecimal importePagos;

    @Money
    @LabelFormat(LabelFormatType.SMALL)
    @Depends("importePagos, cantidadPagos")
    public BigDecimal getMontoTotalPagos() {
        if (importePagos == null || cantidadPagos <= 0) {
            return BigDecimal.ZERO;
        }
        return importePagos.multiply(BigDecimal.valueOf(cantidadPagos));
    }

   @NoDefaultActions
   @RemoveAction("")
    @NewAction("")
    @DeleteSelectedAction("")
    @OneToMany(mappedBy = "registradora")
 // @JsonIgnore // Evita serialización de la lista de cajas para prevenir ciclos.
    @OrderColumn
    @ListProperties("denominacion.nombre, cantidad, total")
    @SearchListTab("sinNull")
    @NotAudited
    private List<Caja> caja;

    @Enumerated(EnumType.STRING)
    @LabelFormat(LabelFormatType.SMALL)
    @Required
    private EstrategiaPagos estrategiaPagos;

    @Money
    @LargeDisplay(icon = "cash-multiple")
    public BigDecimal getTotalSaldo() {
        if (caja == null || caja.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return caja.stream()
                .map(Caja::getTotalGeneral)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @LargeDisplay(icon = "cash-multiple")
    public int getTotalCantidad() {
        if (caja == null || caja.isEmpty()) {
            return 0;
        }
        return caja.stream()
                .mapToInt(Caja::getCantidad)
                .sum();
    } 

    public void ejecutarEstrategiaPagos() {
        if (estrategiaPagos == null) {
            throw new IllegalArgumentException("Debe seleccionar una estrategia de pagos.");
        }

        switch (estrategiaPagos) {
            case MAYOR_CANTIDAD:
                ejecutarMayorCantidad();
                break;
            case MAYOR_VALOR:
                ejecutarMayorValor();
                break;
            case MENOR_VALOR:
                ejecutarMenorValor();
                break;
            case EQUITATIVO:
                ejecutarDistribucionEquitativa();
                break;
            default:
                throw new UnsupportedOperationException("Estrategia no soportada: " + estrategiaPagos);
        }
    }

    @HtmlText(simple = true)
    @Depends("cantidadPagos,importePagos,estrategiaPagos")
    public String getDetallePagos() {
        if (cantidadPagos <= 0 || importePagos == null || importePagos.compareTo(BigDecimal.ZERO) <= 0) {
            return "Defina una cantidad de pagos y un importe válido.";
        }

        if (estrategiaPagos == null) {
            return "Debe seleccionar una estrategia de pagos.";
        }

        StringBuilder detalle = new StringBuilder();
        detalle.append(String.format("Importe por pago: %s\n", importePagos));
        detalle.append(String.format("Cantidad de pagos: %d\n", cantidadPagos));
        detalle.append(String.format("Importe Total a Pagar: %s\n", getMontoTotalPagos()));

        switch (estrategiaPagos) {
            case MAYOR_CANTIDAD:
                detalle.append(ejecutarMayorCantidad());
                break;
            case MAYOR_VALOR:
                detalle.append(ejecutarMayorValor());
                break;
            case MENOR_VALOR:
                detalle.append(ejecutarMenorValor());
                break;
            case EQUITATIVO:
                detalle.append(ejecutarDistribucionEquitativa());
                break;
            default:
                detalle.append("Estrategia no soportada.");
        }

        return detalle.toString();
    }

    private String ejecutarMayorCantidad() {
        return ejecutarEstrategia(
                Comparator.comparing(Caja::getCantidad, Comparator.reverseOrder()),
                "Mayor Cantidad de Billetes"
        );
    }

    private String ejecutarMayorValor() {
        return ejecutarEstrategia(
                Comparator.comparing(c -> c.getDenominacion().getValor(), Comparator.reverseOrder()),
                "Mayor Valor de Billetes"
        );
    }

    private String ejecutarMenorValor() {
        return ejecutarEstrategia(
                Comparator.comparing(c -> c.getDenominacion().getValor()),
                "Menor Valor de Billetes"
        );
    }

    private String ejecutarDistribucionEquitativa() {
        StringBuilder detalle = new StringBuilder("Estrategia: Distribución Equitativa\n");
        validarCaja();

        BigDecimal montoPorPago = importePagos.divide(BigDecimal.valueOf(cantidadPagos), RoundingMode.DOWN);

        for (int i = 1; i <= cantidadPagos; i++) {
            detalle.append(String.format("Distribución para el pago %d:\n", i));
            BigDecimal montoRestante = montoPorPago;

            for (Caja cajaItem : caja) {
                String nombreDenominacion = cajaItem.getDenominacion().getNombre();
                BigDecimal valorDenominacion = cajaItem.getDenominacion().getValor();
                int cantidadDisponible = cajaItem.getCantidad();

                if (cantidadDisponible > 0) {
                    int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                    int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                    cajaItem.setCantidad(cantidadDisponible - billetesUtilizados);

                    if (billetesUtilizados > 0) {
                        detalle.append(String.format("  Denominación %s: %d billetes utilizados\n", nombreDenominacion, billetesUtilizados));
                    }

                    if (montoRestante.compareTo(BigDecimal.ZERO) == 0) {
                        break;
                    }
                }
            }

            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                detalle.append(String.format("  No se pudo cubrir el monto restante: %s\n", montoRestante));
            }
        }

        return detalle.toString();
    }

    private String ejecutarEstrategia(Comparator<Caja> comparator, String estrategia) {
        validarCaja();
        StringBuilder detalle = new StringBuilder("Estrategia: " + estrategia + "\n");

        caja.sort(comparator);

        for (int i = 1; i <= cantidadPagos; i++) {
            detalle.append(String.format("Distribución para el pago %d:\n", i));
            BigDecimal montoRestante = importePagos;

            for (Caja cajaItem : caja) {
                String nombreDenominacion = cajaItem.getDenominacion().getNombre();
                BigDecimal valorDenominacion = cajaItem.getDenominacion().getValor();
                int cantidadDisponible = cajaItem.getCantidad();

                int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                cajaItem.setCantidad(cantidadDisponible - billetesUtilizados);

                if (billetesUtilizados > 0) {
                    detalle.append(String.format("  Denominación %s: %d billetes utilizados\n", nombreDenominacion, billetesUtilizados));
                }

                if (montoRestante.compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
            }

            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                detalle.append(String.format("  No se pudo cubrir el monto restante: %s\n", montoRestante));
            }
        }

        return detalle.toString();
    }

    private void validarCaja() {
        if (caja == null || caja.isEmpty()) {
            throw new IllegalStateException("No hay datos disponibles en la caja para realizar pagos.");
        }
        for (Caja item : caja) {
            if (item.getDenominacion() == null || item.getCantidad() <= 0) {
                throw new IllegalStateException("La caja contiene datos inválidos (denominación nula o cantidad inválida).");
            }
        }
    }
}
