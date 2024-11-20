package com.sta.cajadepagos.modelo;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.jpa.*;
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
editors ="List, Charts",
defaultOrder = "${fechaHora} desc")


@Entity
@Getter @Setter
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
    @Depends("importePagos, cantidadPagos") // Asegura que se recalcula al cambiar estas propiedades
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
    @OrderColumn
    @ListProperties("denominacion.nombre, cantidad, total")
    @SearchListTab("sinNull")
    private List<Caja> caja;

    @Enumerated(EnumType.STRING)
    @LabelFormat(LabelFormatType.SMALL)
    @Required
    private EstrategiaPagos estrategiaPagos;

    @LargeDisplay(icon = "cash-multiple")
    public int getTotalCantidad() {
        try {
            return XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(c.cantidad), 0) FROM Caja c", Long.class)
                .getSingleResult()
                .intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // En caso de error
        }
    }

    @Money
    @LargeDisplay
    public BigDecimal getTotalSaldo() {
        try {
            return XPersistence.getManager()
                .createQuery("SELECT COALESCE(SUM(c.total), 0) FROM Caja c", BigDecimal.class)
                .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO; // En caso de error
        }
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

    @HtmlText(simple=true)
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
        detalle.append(String.format("Importe Total a Pagar: %s\n", getMontoTotalPagos())); // Agregar el total de pagos
     //   detalle.append(String.format("Estrategia seleccionada: %s\n", estrategiaPagos.getEstrategiaPagos()));
        
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
        StringBuilder detalle = new StringBuilder("Estrategia: Mayor Cantidad de Billetes\n");
        Map<String, Integer> cantidadesDisponibles = inicializarCantidadesDisponibles();

        // Ordenamos las cajas por cantidad disponible en orden descendente
        caja.sort(Comparator.comparing(Caja::getCantidad, Comparator.reverseOrder()));

        // Iteramos por cada pago
        for (int i = 1; i <= cantidadPagos; i++) {
            detalle.append(String.format("Distribución para el pago %d:\n", i));
            BigDecimal montoRestante = importePagos;

            // Intentamos cubrir el monto restante con las denominaciones disponibles
            for (Caja cajaItem : caja) {
                String nombreDenominacion = cajaItem.getDenominacion().getNombre();
                BigDecimal valorDenominacion = cajaItem.getDenominacion().getValor();
                int cantidadDisponible = cantidadesDisponibles.getOrDefault(nombreDenominacion, 0);

                if (cantidadDisponible > 0 && montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                    // Calculamos cuántos billetes de esta denominación se necesitan
                    int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                    int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                    // Actualizamos el monto restante y las cantidades disponibles
                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                    cantidadesDisponibles.put(nombreDenominacion, cantidadDisponible - billetesUtilizados);

                    if (billetesUtilizados > 0) {
                        detalle.append(String.format("  Denominación %s: %d billetes utilizados\n", nombreDenominacion, billetesUtilizados));
                    }
                }
            }

            // Verificamos si no se pudo cubrir el monto restante
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                detalle.append(String.format("  No se pudo cubrir el monto restante: %s\n", montoRestante));
            }
        }

        return detalle.toString();
    }

    private String ejecutarMayorValor() {
        StringBuilder detalle = new StringBuilder("Estrategia: Mayor Valor de Billetes\n");
        Map<String, Integer> cantidadesDisponibles = inicializarCantidadesDisponibles();
        caja.sort(Comparator.comparing(c -> c.getDenominacion().getValor(), Comparator.reverseOrder()));
        distribuirBilletes(detalle, cantidadesDisponibles);
        return detalle.toString();
    }

    private String ejecutarMenorValor() {
        StringBuilder detalle = new StringBuilder("Estrategia: Menor Valor de Billetes\n");
        Map<String, Integer> cantidadesDisponibles = inicializarCantidadesDisponibles();
        caja.sort(Comparator.comparing(c -> c.getDenominacion().getValor()));
        distribuirBilletes(detalle, cantidadesDisponibles);
        return detalle.toString();
    }

    private String ejecutarDistribucionEquitativa() {
        StringBuilder detalle = new StringBuilder("Estrategia: Distribución Equitativa\n");
        Map<String, Integer> cantidadesDisponibles = inicializarCantidadesDisponibles();

        for (int i = 1; i <= cantidadPagos; i++) {
            detalle.append(String.format("Distribución para el pago %d:\n", i));
            BigDecimal montoRestante = importePagos;

            for (Caja cajaItem : caja) {
                String nombreDenominacion = cajaItem.getDenominacion().getNombre();
                BigDecimal valorDenominacion = cajaItem.getDenominacion().getValor();
                int cantidadDisponible = cantidadesDisponibles.getOrDefault(nombreDenominacion, 0);

                if (cantidadDisponible > 0) {
                    int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                    int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                    cantidadesDisponibles.put(nombreDenominacion, cantidadDisponible - billetesUtilizados);

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

    private Map<String, Integer> inicializarCantidadesDisponibles() {
        Map<String, Integer> cantidadesDisponibles = new HashMap<>();
        for (Caja cajaItem : caja) {
            if (cajaItem.getDenominacion() != null) {
                cantidadesDisponibles.put(cajaItem.getDenominacion().getNombre(), cajaItem.getCantidad());
            }
        }
        return cantidadesDisponibles;
    }

    private void distribuirBilletes(StringBuilder detalle, Map<String, Integer> cantidadesDisponibles) {
        for (int i = 1; i <= cantidadPagos; i++) {
            detalle.append(String.format("Distribución para el pago %d:\n", i));
            BigDecimal montoRestante = importePagos;

            for (Caja cajaItem : caja) {
                String nombreDenominacion = cajaItem.getDenominacion().getNombre();
                BigDecimal valorDenominacion = cajaItem.getDenominacion().getValor();
                int cantidadDisponible = cantidadesDisponibles.getOrDefault(nombreDenominacion, 0);

                int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                cantidadesDisponibles.put(nombreDenominacion, cantidadDisponible - billetesUtilizados);

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
    }
}
