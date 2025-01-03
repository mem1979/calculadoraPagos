package com.sta.cashstill.acciones;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.actions.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.modelo.*;

public class CajaSalidaCalcularDetalleAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        try {
            CajaSalida cajaSalida = (CajaSalida) getView().getEntity();

            // Validar importe
            if (!validarImporte(cajaSalida)) {
                return; // Detener ejecución si hay un error de validación
            }

            // Calcular el total disponible
            BigDecimal totalDisponible = calcularTotalDisponible();

            // Validar disponibilidad
            if (!validarDisponibilidad(cajaSalida, totalDisponible)) {
                return; // Detener ejecución si no hay suficiente dinero disponible
            }

            // Realizar el cálculo
            List<DetalleCajaRegistradora> nuevosDetalles = calcularDetalles(cajaSalida);

            // Asignar resultados a la vista
            asignarResultados(nuevosDetalles);

            // Mensaje de éxito
            addMessage("Cálculo realizado correctamente.");
        } catch (Exception e) {
            addError("Se produjo un error inesperado: " + e.getMessage());
        }
    }

    private boolean validarImporte(CajaSalida cajaSalida) {
        if (cajaSalida.getImporte() == null || cajaSalida.getImporte().compareTo(BigDecimal.ZERO) == 0) {
            addError("Debe ingresar un Importe para realizar el Pago");
            return false;
        }
        return true;
    }

    private boolean validarDisponibilidad(CajaSalida cajaSalida, BigDecimal totalDisponible) {
        if (cajaSalida.getImporte().compareTo(totalDisponible) > 0) {
            addError("El total disponible en la caja no es suficiente para cubrir el importe del pago. Disponible: " 
                + totalDisponible + ", Requerido: " + cajaSalida.getImporte());
            return false;
        }
        return true;
    }

    private List<DetalleCajaRegistradora> calcularDetalles(CajaSalida cajaSalida) throws Exception {
        BigDecimal montoRestante = cajaSalida.getImporte();
        List<Caja> cajas = obtenerCajasOrdenadas();

        List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();
        Map<Caja, Integer> copiaCantidadesDisponibles = new HashMap<>();

        for (Caja caja : cajas) {
            copiaCantidadesDisponibles.put(caja, caja.getCantidad());
        }

        // Primera pasada: estrategia optimizada
        aplicarEstrategiaOptimizada(montoRestante, cajas, nuevosDetalles, copiaCantidadesDisponibles);

        // Validar si queda algún monto restante
        montoRestante = montoRestante.subtract(
            nuevosDetalles.stream()
                .map(DetalleCajaRegistradora::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        // Segunda pasada: billetes menores
        if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
            aplicarSegundaPasada(montoRestante, cajas, nuevosDetalles, copiaCantidadesDisponibles);
        }

        // Unificar y ordenar detalles
        if (!nuevosDetalles.isEmpty()) {
            nuevosDetalles = unificarDetalles(nuevosDetalles);
            nuevosDetalles.sort(Comparator.comparing((DetalleCajaRegistradora detalle) -> detalle.getCaja().getValor()).reversed());
        }

        return nuevosDetalles;
    }

    private void asignarResultados(List<DetalleCajaRegistradora> nuevosDetalles) throws Exception {
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
            detallesMap.add(detalleMap);
        }

        getView().setValue("detalle", detallesMap);
        getView().refreshCollections(); // Refrescar solo la colección de detalles
    }

    private BigDecimal calcularTotalDisponible() {
        String query = "SELECT SUM(c.valor * c.cantidad) FROM Caja c WHERE c.cantidad > 0";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        BigDecimal totalDisponible = (BigDecimal) manager.createQuery(query).getSingleResult();
        return totalDisponible != null ? totalDisponible : BigDecimal.ZERO;
    }

    private List<Caja> obtenerCajasOrdenadas() {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ORDER BY c.valor DESC";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        return manager.createQuery(query, Caja.class).getResultList();
    }

    private void aplicarEstrategiaOptimizada(BigDecimal montoRestante, List<Caja> cajas, List<DetalleCajaRegistradora> detalles, Map<Caja, Integer> copiaCantidadesDisponibles) {
        cajas.sort(Comparator.comparing((Caja c) -> c.getValor()).reversed()
                .thenComparing(Caja::getCantidad, Comparator.reverseOrder()));

        for (Caja caja : cajas) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getValor();
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

    private void aplicarSegundaPasada(BigDecimal montoRestante, List<Caja> cajas, List<DetalleCajaRegistradora> detalles, Map<Caja, Integer> copiaCantidadesDisponibles) {
        for (Caja caja : cajas) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getValor();
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
}
