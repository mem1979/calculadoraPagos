package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.util.*;

import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;

public class CajaSalidaGenerarDetalleAction extends ViewBaseAction {

    @SuppressWarnings("unchecked")
	@Override
    public void execute() throws Exception {
        try {
            // Obtener la entidad CajaSalida desde la vista
            CajaSalida cajaSalida = (CajaSalida) getView().getEntity();

            // Calcular el total disponible en la caja
            BigDecimal totalDisponible = calcularTotalDisponible();

            // Verificar si el total disponible es suficiente para cubrir el importe
            if (cajaSalida.getImporte().compareTo(totalDisponible) > 0) {
                addError("El total disponible en la caja no es suficiente para cubrir el importe del pago. Disponible: " + totalDisponible + ", Requerido: " + cajaSalida.getImporte());
                return;
            }

            // Verificar que la estrategia sea MEJOR_AJUSTE
            if (cajaSalida.getEstrategiaPagos() != EstrategiaPagos.MEJOR_AJUSTE) {
                addError("Seleccione la estrategia de pagos Calculada, para generar el detalle optimizando los billetes y las denominaciones disponibles");
                return;
            }

            BigDecimal montoRestante = cajaSalida.getImporte();
            List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();

            // Obtener las cajas disponibles ordenadas dinámicamente
            List<Caja> cajas = obtenerCajasOrdenadas();

            // Crear una copia de las cantidades disponibles para simulación sin afectar la base de datos
            Map<Caja, Integer> copiaCantidadesDisponibles = new HashMap<>();
            for (Caja caja : cajas) {
                copiaCantidadesDisponibles.put(caja, caja.getCantidad());
            }

            // Primera pasada: Aplicar estrategia optimizada priorizando cubrir el importe
            aplicarEstrategiaOptimizada(montoRestante, cajas, nuevosDetalles, copiaCantidadesDisponibles);

            // Validar si queda algún monto restante
            montoRestante = montoRestante.subtract(
                nuevosDetalles.stream()
                    .map(DetalleCajaRegistradora::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            );

            // Segunda pasada: Intentar cubrir el monto restante con billetes menores
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                aplicarSegundaPasada(montoRestante, cajas, nuevosDetalles, copiaCantidadesDisponibles);
            }

            // Validar y unificar los detalles antes de asignarlos a la vista
            if (!nuevosDetalles.isEmpty()) {
                nuevosDetalles = unificarDetalles(nuevosDetalles);

                // Ordenar los detalles de mayor a menor valor
                nuevosDetalles.sort(Comparator.comparing(
                    (DetalleCajaRegistradora detalle) -> detalle.getCaja().getDenominacion().getValor())
                    .reversed()
                );
            }

            // Convertir los detalles a mapas para asignarlos a la vista
            List<Map<String, Object>> detallesMap = new ArrayList<>();
            for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                if (detalle == null || detalle.getCaja() == null || detalle.getCantidad() == 0 || detalle.getTotal() == null) {
                    addError("Uno de los detalles está incompleto o contiene datos nulos. Verifique los valores ingresados.");
                    return;
                }

                Map<String, Object> detalleMap = new HashMap<>();
                detalleMap.put("caja.id", detalle.getCaja().getId());
                detalleMap.put("cantidad", detalle.getCantidad());
                detalleMap.put("total", detalle.getTotal());
                detalleMap = Maps.plainToTree(detalleMap);
                detallesMap.add(detalleMap);
            }

            // Asignar la colección convertida a la vista
            getView().setValue("detalle", detallesMap);

            // Refrescar la colección
            getView().refreshCollections();

            // Mensaje de éxito
            addMessage("Cálculo realizado correctamente.");
        } catch (Exception e) {
            addError("Se produjo un error al completar la colección de detalles: " + e.getMessage());
        }
    }

    /**
     * Aplicar la estrategia MEJOR_AJUSTE optimizada (primera pasada).
     * Prioriza billetes de mayor denominación y mayor cantidad disponible,
     * asegurándose de no asignar más de lo disponible.
     */
    private void aplicarEstrategiaOptimizada(BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles, Map<Caja, Integer> copiaCantidadesDisponibles) {
        cajasDisponibles.sort(Comparator.comparing((Caja c) -> c.getDenominacion().getValor()).reversed()
                .thenComparing(Caja::getCantidad, Comparator.reverseOrder()));

        for (Caja caja : cajasDisponibles) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getDenominacion().getValor();
            int cantidadDisponible = copiaCantidadesDisponibles.get(caja);

            if (cantidadDisponible <= 0) continue;

            int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
            int cantidadUsada = Math.min(cantidadRequerida, cantidadDisponible);

            if (cantidadUsada > 0) {
                DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                detalle.setCaja(caja);
                detalle.setCantidad(cantidadUsada);
                detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                detalles.add(detalle);

                montoRestante = montoRestante.subtract(detalle.getTotal());
                copiaCantidadesDisponibles.put(caja, cantidadDisponible - cantidadUsada);
            }
        }
    }

    /**
     * Segunda pasada para cubrir el monto restante con billetes más pequeños.
     */
    private void aplicarSegundaPasada(BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles, Map<Caja, Integer> copiaCantidadesDisponibles) {
        for (Caja caja : cajasDisponibles) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getDenominacion().getValor();
            int cantidadDisponible = copiaCantidadesDisponibles.get(caja);

            if (cantidadDisponible <= 0) continue;

            int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
            int cantidadUsada = Math.min(cantidadRequerida, cantidadDisponible);

            if (cantidadUsada > 0) {
                DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                detalle.setCaja(caja);
                detalle.setCantidad(cantidadUsada);
                detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                detalles.add(detalle);

                montoRestante = montoRestante.subtract(detalle.getTotal());
                copiaCantidadesDisponibles.put(caja, cantidadDisponible - cantidadUsada);
            }
        }
    }

    /**
     * Unificar los detalles por denominación.
     */
    private List<DetalleCajaRegistradora> unificarDetalles(List<DetalleCajaRegistradora> detalles) {
        Map<Caja, DetalleCajaRegistradora> detalleMap = new HashMap<>();

        for (DetalleCajaRegistradora detalle : detalles) {
            Caja caja = detalle.getCaja();
            if (detalleMap.containsKey(caja)) {
                DetalleCajaRegistradora existente = detalleMap.get(caja);
                existente.setCantidad(existente.getCantidad() + detalle.getCantidad());
                existente.setTotal(existente.getTotal().add(detalle.getTotal()));
            } else {
                detalleMap.put(caja, detalle);
            }
        }

        return new ArrayList<>(detalleMap.values());
    }

    /**
     * Calcular el total de dinero disponible en las cajas.
     */
    private BigDecimal calcularTotalDisponible() {
        String query = "SELECT SUM(c.denominacion.valor * c.cantidad) FROM Caja c WHERE c.cantidad > 0";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        BigDecimal totalDisponible = (BigDecimal) manager.createQuery(query).getSingleResult();
        return totalDisponible != null ? totalDisponible : BigDecimal.ZERO;
    }

    /**
     * Obtener las cajas disponibles para MEJOR_AJUSTE, ordenadas dinámicamente.
     */
    private List<Caja> obtenerCajasOrdenadas() {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        return manager.createQuery(query, Caja.class).getResultList();
    }
}
