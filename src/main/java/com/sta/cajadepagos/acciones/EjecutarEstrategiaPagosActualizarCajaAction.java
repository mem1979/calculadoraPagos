package com.sta.cajadepagos.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cajadepagos.modelo.*;

public class EjecutarEstrategiaPagosActualizarCajaAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        Registradora registradora = (Registradora) getView().getEntity();
        try {
            // Ejecutar la estrategia seleccionada
            registradora.ejecutarEstrategiaPagos();

            // Mapa de cantidades utilizadas por denominación
            Map<String, Integer> cantidadesUtilizadas = calcularCantidadesUtilizadas(registradora);

            // Actualizar las cantidades y totales de la clase Caja
            for (Caja caja : registradora.getCaja()) {
                if (caja.getDenominacion() != null) {
                    String denominacion = caja.getDenominacion().getNombre();
                    int cantidadDisponible = caja.getCantidad();
                    int cantidadUsada = cantidadesUtilizadas.getOrDefault(denominacion, 0);

                    // Restar la cantidad utilizada y recalcular el total
                    caja.setCantidad(cantidadDisponible - cantidadUsada);
                    BigDecimal nuevoTotal = caja.getDenominacion().getValor()
                            .multiply(BigDecimal.valueOf(caja.getCantidad()));
                    caja.setTotal(nuevoTotal);

                    // Persistir la entidad Caja
                    XPersistence.getManager().merge(caja);
                }
            }

            // Persistir la entidad Registradora
            XPersistence.getManager().merge(registradora);

            addMessage("Estrategia de pagos ejecutada y cantidades actualizadas correctamente.");
        } catch (Exception e) {
            addError("Error al ejecutar la estrategia: " + e.getMessage());
        }
    }

    /**
     * Calcula las cantidades utilizadas para cada denominación en función de la estrategia de la registradora.
     */
    private Map<String, Integer> calcularCantidadesUtilizadas(Registradora registradora) {
        Map<String, Integer> cantidadesUtilizadas = new HashMap<>();

        for (int i = 1; i <= registradora.getCantidadPagos(); i++) {
            BigDecimal montoRestante = registradora.getImportePagos();

            for (Caja caja : registradora.getCaja()) {
                if (caja.getDenominacion() != null) {
                    String nombreDenominacion = caja.getDenominacion().getNombre();
                    BigDecimal valorDenominacion = caja.getDenominacion().getValor();
                    int cantidadDisponible = caja.getCantidad();

                    // Calcular cuántos billetes se usarán para cubrir el monto restante
                    int billetesRequeridos = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                    int billetesUtilizados = Math.min(billetesRequeridos, cantidadDisponible);

                    // Restar del monto restante y agregar al mapa de cantidades utilizadas
                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(billetesUtilizados)));
                    cantidadesUtilizadas.put(nombreDenominacion,
                            cantidadesUtilizadas.getOrDefault(nombreDenominacion, 0) + billetesUtilizados);

                    // Si el monto restante ya está cubierto, terminar
                    if (montoRestante.compareTo(BigDecimal.ZERO) == 0) {
                        break;
                    }
                }
            }
        }

        return cantidadesUtilizadas;
    }
}
