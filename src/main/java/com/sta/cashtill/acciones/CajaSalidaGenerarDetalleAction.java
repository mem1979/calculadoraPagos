package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;

import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;

public class CajaSalidaGenerarDetalleAction extends ViewBaseAction {

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

            // Obtener las cajas ordenadas según la estrategia de pago
            List<Caja> cajas = obtenerCajasOrdenadas(estrategiaPagos);
            System.out.println(">>> Cajas obtenidas: " + cajas.size());

            BigDecimal montoRestante = cajaSalida.getMontoTotalPagos();
            System.out.println(">>> Monto total a pagar: " + montoRestante);

            // Crear una nueva colección de detalles
            List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();

            // Generar los detalles para cubrir el monto total
            for (Caja caja : cajas) {
                if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal valorDenominacion = caja.getDenominacion().getValor();
                int cantidadDisponible = caja.getCantidad();
                int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                int cantidadUsada = Math.min(cantidadRequerida, cantidadDisponible);

                if (cantidadUsada > 0) {
                    // Crear el detalle y agregarlo a la colección
                    DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                    detalle.setCaja(caja);
                    detalle.setCantidad(cantidadUsada);
                    detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                    nuevosDetalles.add(detalle);

                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                    System.out.println(">>> Detalle generado: Caja=" + caja.getDenominacion().getValor() +
                                       ", Cantidad=" + cantidadUsada + ", Total=" + detalle.getTotal()); }
            }

            // Verificar si se cubrió el monto total
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                addError("No hay suficientes billetes para cubrir el monto total requerido.");
                return;
            }

            // Convertir los detalles a mapas para asignarlos a la vista
            List<Map<String, Object>> detallesMap = new ArrayList<>();
            for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                Map<String, Object> detalleMap = new HashMap<>();
                detalleMap.put("cajaId", detalle.getCaja().getId() );
                detalleMap.put("cantidad", detalle.getCantidad());
                detalleMap.put("total", detalle.getTotal());
                detallesMap.add(detalleMap);

                // Depuración para verificar cada mapa generado
                System.out.println(">>> Detalle convertido a Map: " + detalleMap);
                System.out.println(detalle.getCajaId());
            }

            // Asignar la colección convertida a la vista
            getView().setValue("detalle", detallesMap);
            System.out.println(">>> La colección de detalles fue asignada a la vista.");

            // Mensaje de éxito
            addMessage("La colección de detalles ha sido completada correctamente.");
        } catch (Exception e) {
            // Capturar y mostrar cualquier error
            System.err.println(">>> Error en CajaSalidaGenerarDetalleAction: " + e.getMessage());
            e.printStackTrace();
            addError("Se produjo un error al completar la colección de detalles. Consulte la consola para más detalles.");
        }
    }

    /**
     * Obtener las cajas disponibles según la estrategia de pago seleccionada.
     */
    private List<Caja> obtenerCajasOrdenadas(EstrategiaPagos estrategia) {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ORDER BY ";
        switch (estrategia) {
            case MAYOR_CANTIDAD:
                query += "c.cantidad DESC";
                break;
            case MENOR_VALOR:
                query += "c.denominacion.valor ASC";
                break;
            case MAYOR_VALOR:
                query += "c.denominacion.valor DESC";
                break;
            default:
                query += "c.cantidad DESC";
        }
        return org.openxava.jpa.XPersistence.getManager().createQuery(query, Caja.class).getResultList();
    }
}

