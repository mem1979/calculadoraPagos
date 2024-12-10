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

             // Verificar la estrategia de pago seleccionada
             EstrategiaPagos estrategiaPagos = cajaSalida.getEstrategiaPagos();
             if (estrategiaPagos == null) {
                 addError("Debe seleccionar una estrategia de pago antes de generar el detalle.");
                 return;
             }

             BigDecimal montoRestante = cajaSalida.getImporte();
             List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();

             // Procesar según la estrategia seleccionada
             switch (estrategiaPagos) {
                 case MANUAL:
                     aplicarEstrategiaManual(cajaSalida, nuevosDetalles);
                     break;
                 case MEJOR_AJUSTE:
                     List<Caja> cajas = obtenerCajasOrdenadas(montoRestante);
                     aplicarEstrategiaOptimizada(montoRestante, cajas, nuevosDetalles);
                     break;
                 default:
                     addError("Estrategia de pago no soportada.");
                     return;
             }

             // Unificar los detalles antes de asignarlos a la vista
             nuevosDetalles = unificarDetalles(nuevosDetalles);

             // **Ordenar los detalles de mayor a menor valor**
             nuevosDetalles.sort(Comparator.comparing(
                 (DetalleCajaRegistradora detalle) -> detalle.getCaja().getDenominacion().getValor())
                 .reversed()
             );

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
             cajaSalida.setDescripcion(descriptionBuilder.toString());
             getView().setValue("descripcion", descriptionBuilder.toString());

             // Convertir los detalles a mapas para asignarlos a la vista
             List<Map<String, Object>> detallesMap = new ArrayList<>();
             for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                 Map<String, Object> detalleMap = new HashMap<>();
                 detalleMap.put("cajaSearch.id", detalle.getCaja().getId());
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
             addMessage("La colección de detalles ha sido completada correctamente.");
         } catch (Exception e) {
             addError("Se produjo un error al completar la colección de detalles: " + e.getMessage());
         }
     }

     /**
      * Aplicar la estrategia MANUAL.
      */
     private void aplicarEstrategiaManual(CajaSalida cajaSalida, List<DetalleCajaRegistradora> detalles) {
         List<DetalleCajaRegistradora> seleccionados = cajaSalida.getDetalle();
         if (seleccionados == null || seleccionados.isEmpty()) {
             addError("No se seleccionaron denominaciones para la estrategia manual.");
             return;
         }

         detalles.addAll(seleccionados);
     }

     /**
      * Aplicar la estrategia MEJOR_AJUSTE utilizando el método optimizado.
      */
     private void aplicarEstrategiaOptimizada(BigDecimal montoRestante, List<Caja> cajasDisponibles, List<DetalleCajaRegistradora> detalles) {
         final double porcentajeMinimoCambio = 0.2; // Conservar al menos el 20% de cada denominación

         // Ordenar las cajas por valor de denominación (mayor a menor) y luego por cantidad disponible (mayor a menor)
         cajasDisponibles.sort(Comparator.comparing((Caja c) -> c.getDenominacion().getValor()).reversed()
                 .thenComparing(Caja::getCantidad, Comparator.reverseOrder()));

         for (Caja caja : cajasDisponibles) {
             if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

             BigDecimal valorDenominacion = caja.getDenominacion().getValor();
             int cantidadDisponible = caja.getCantidad();

             if (cantidadDisponible <= 0) continue;

             // Calcular cuántos billetes se pueden usar manteniendo el nivel mínimo de cambio
             int cantidadMinimaCambio = (int) Math.ceil(cantidadDisponible * porcentajeMinimoCambio);
             int cantidadPermitida = Math.max(0, cantidadDisponible - cantidadMinimaCambio);

             // Calcular cuántos billetes se necesitan para cubrir el monto restante
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
      * Obtener las cajas disponibles para MEJOR_AJUSTE, ordenadas dinámicamente.
      */
     private List<Caja> obtenerCajasOrdenadas(BigDecimal montoTotal) {
         String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC, c.cantidad DESC";
         EntityManager manager = org.openxava.jpa.XPersistence.getManager();
         return manager.createQuery(query, Caja.class).getResultList();
     }
 }
