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
            System.out.println(">>> CajaSalida obtenida desde la vista: " + cajaSalida);

            // Verificar la estrategia de pago seleccionada
            EstrategiaPagos estrategiaPagos = cajaSalida.getEstrategiaPagos();
            if (estrategiaPagos == null) {
                addError("Debe seleccionar una estrategia de pago antes de generar el detalle.");
                return;
            }
            System.out.println(">>> Estrategia de Pagos Seleccionada: " + estrategiaPagos);

            BigDecimal montoRestante = cajaSalida.getImporte();
            List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();

            // Procesar según la estrategia seleccionada
            switch (estrategiaPagos) {
                case MANUAL:
                    aplicarEstrategiaManual(cajaSalida, nuevosDetalles);
                    break;
                default:
                    // Obtener las cajas ordenadas según la estrategia de pago
                    List<Caja> cajas = obtenerCajasOrdenadas(estrategiaPagos, montoRestante);
                    aplicarEstrategiaConPorcentaje(estrategiaPagos, montoRestante, cajas, nuevosDetalles);
                    break;
            }

            // Verificar si se cubrió completamente el importe
            BigDecimal totalCubierto = nuevosDetalles.stream()
                    .map(DetalleCajaRegistradora::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            montoRestante = montoRestante.subtract(totalCubierto);

            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                addWarning("No se pudo cubrir el monto total requerido. Monto restante: " + montoRestante);
            }

            // Generar la descripción
            StringBuilder descriptionBuilder = new StringBuilder();
            for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                descriptionBuilder.append("Cantidad ")
                        .append(detalle.getCantidad())
                        .append(" de billetes de valor ")
                        .append(detalle.getCaja().getDenominacion().getValor())
                        .append("\n");
            }

            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                descriptionBuilder.append("No se pudo cubrir el monto total requerido. Monto restante: ")
                        .append(montoRestante)
                        .append("\n");
            }

            // Asignar la descripción generada
            String nuevaDescripcion = descriptionBuilder.toString();
            cajaSalida.setDescripcion(nuevaDescripcion);
            getView().setValue("descripcion", nuevaDescripcion);

            // Convertir los detalles a mapas para asignarlos a la vista
            List<Map<String, Object>> detallesMap = new ArrayList<>();
            for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                Map<String, Object> detalleMap = new HashMap<>();
                detalleMap.put("caja.id", detalle.getCaja().getId());
                detalleMap.put("cantidad", detalle.getCantidad());
                detalleMap.put("total", detalle.getTotal());
                detalleMap = Maps.plainToTree(detalleMap);
                detallesMap.add(detalleMap);
            }

            // Asignar la colección convertida a la vista
            getView().setValue("detalle", detallesMap);
            System.out.println(">>> La colección de detalles fue asignada a la vista.");

            // Refrescar únicamente la colección
            getView().refreshCollections();

            // Mensaje de éxito
            addMessage("La colección de detalles ha sido completada correctamente.");
        } catch (Exception e) {
            System.err.println(">>> Error en CajaSalidaGenerarDetalleAction: " + e.getMessage());
            e.printStackTrace();
            addError("Se produjo un error al completar la colección de detalles. Consulte la consola para más detalles.");
        }
    }

    /**
     * Obtener las cajas disponibles según la estrategia de pago seleccionada.
     */
    private List<Caja> obtenerCajasOrdenadas(EstrategiaPagos estrategia, BigDecimal montoTotal) {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ";
        switch (estrategia) {
            case MAYOR_CANTIDAD:
                query += "ORDER BY c.cantidad DESC";
                break;
            case MENOR_VALOR:
                query += "ORDER BY c.denominacion.valor ASC";
                break;
            case MAYOR_VALOR:
                query += "ORDER BY c.denominacion.valor DESC";
                break;
            case MEJOR_AJUSTE:
                query += "ORDER BY ABS(c.denominacion.valor - :montoTotal) ASC";
                break;
            default:
                query += "ORDER BY c.cantidad DESC";
        }

        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        TypedQuery<Caja> jpaQuery = manager.createQuery(query, Caja.class);
        if (estrategia == EstrategiaPagos.MEJOR_AJUSTE) {
            jpaQuery.setParameter("montoTotal", montoTotal);
        }
        return jpaQuery.getResultList();
    }

    /**
     * Aplicar estrategia MANUAL.
     */
    private void aplicarEstrategiaManual(CajaSalida cajaSalida, List<DetalleCajaRegistradora> detalles) {
        // Obtener las denominaciones y cantidades seleccionadas por el usuario desde la colección
        List<DetalleCajaRegistradora> seleccionados = cajaSalida.getDetalle();
        if (seleccionados == null || seleccionados.isEmpty()) {
            addError("No se seleccionaron denominaciones para la estrategia manual.");
            return;
        }

        for (DetalleCajaRegistradora seleccionado : seleccionados) {
            detalles.add(seleccionado);
            System.out.println(">>> Denominación manual seleccionada: " +
                    seleccionado.getCaja().getDenominacion().getValor() +
                    ", Cantidad: " + seleccionado.getCantidad());
        }
    }

    /**
     * Aplicar estrategia con porcentaje máximo de uso.
     */
    private void aplicarEstrategiaConPorcentaje(EstrategiaPagos estrategia, BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles) {
        switch (estrategia) {
            case MAYOR_CANTIDAD:
            case MAYOR_VALOR:
            case MENOR_VALOR:
                aplicarEstrategiaBasicaConPorcentaje(estrategia, montoRestante, cajasDisponibles, detalles);
                break;
            case MEJOR_AJUSTE:
                aplicarEstrategiaMejorAjuste(montoRestante, cajasDisponibles, detalles);
                break;
            default:
                throw new IllegalArgumentException("Estrategia no soportada: " + estrategia);
        }
    }

    /**
     * Aplicar la estrategia básica con porcentaje máximo de uso.
     */
    private void aplicarEstrategiaBasicaConPorcentaje(EstrategiaPagos estrategia, BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles) {
        final double porcentajeMaximoUso = 0.8; // Usar hasta el 80% de los billetes disponibles inicialmente

        // Ordenar las cajas según la estrategia seleccionada
        switch (estrategia) {
            case MAYOR_CANTIDAD:
                cajasDisponibles.sort(Comparator.comparing(Caja::getCantidad).reversed());
                break;
            case MENOR_VALOR:
                cajasDisponibles.sort(Comparator.comparing(c -> c.getDenominacion().getValor()));
                break;
            case MAYOR_VALOR:
                cajasDisponibles.sort(Comparator.comparing(c -> ((Caja) c).getDenominacion().getValor()).reversed());
                break;
        }

        for (Caja caja : cajasDisponibles) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getDenominacion().getValor();
            int cantidadDisponible = caja.getCantidad();
            int cantidadPermitida = (int) Math.floor(cantidadDisponible * porcentajeMaximoUso);
            int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
            int cantidadUsada = Math.min(cantidadRequerida, cantidadPermitida);

            if (cantidadUsada > 0) {
                DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                detalle.setCaja(caja);
                detalle.setCantidad(cantidadUsada);
                detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                detalles.add(detalle);

                montoRestante = montoRestante.subtract(detalle.getTotal());
            }
        }
    }

    /**
     * Aplicar la estrategia MEJOR_AJUSTE para cubrir el monto restante.
     */
    private void aplicarEstrategiaMejorAjuste(BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles) {
        final double porcentajeMaximoUso = 0.8; // Usar hasta el 80% de los billetes disponibles inicialmente

        // Crear un mapa temporal que asocia cada Caja con su "proximidad" al monto restante
        Map<Caja, BigDecimal> proximidadMap = new HashMap<>();
        for (Caja caja : cajasDisponibles) {
            proximidadMap.put(caja, caja.getDenominacion().getValor().subtract(montoRestante).abs());
        }

        // Ordenar las cajas según la proximidad pre-calculada
        cajasDisponibles.sort(Comparator.comparing(proximidadMap::get));

        for (Caja caja : cajasDisponibles) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal valorDenominacion = caja.getDenominacion().getValor();
            int cantidadDisponible = caja.getCantidad();
            int cantidadPermitida = (int) Math.floor(cantidadDisponible * porcentajeMaximoUso);
            int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
            int cantidadUsada = Math.min(cantidadRequerida, cantidadPermitida);

            if (cantidadUsada > 0) {
                DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                detalle.setCaja(caja);
                detalle.setCantidad(cantidadUsada);
                detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                detalles.add(detalle);

                montoRestante = montoRestante.subtract(detalle.getTotal());
            }
        }
    }
}

