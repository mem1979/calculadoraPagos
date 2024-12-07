package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

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

            // Obtener las cajas ordenadas seg�n la estrategia de pago
            List<Caja> cajas = obtenerCajasOrdenadas(estrategiaPagos);
            System.out.println(">>> Cajas obtenidas: " + cajas.size());

            BigDecimal montoRestante = cajaSalida.getImporte();
            System.out.println(">>> Monto total a pagar: " + montoRestante);

            // Crear una nueva colecci�n de detalles
            List<DetalleCajaRegistradora> nuevosDetalles = new ArrayList<>();

            // StringBuilder para la descripci�n (se reinicia con cada ejecuci�n)
            StringBuilder descriptionBuilder = new StringBuilder();

            // Generar los detalles para cubrir el monto total
            for (Caja caja : cajas) {
                if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal valorDenominacion = caja.getDenominacion().getValor();
                int cantidadDisponible = caja.getCantidad();
                int cantidadRequerida = montoRestante.divide(valorDenominacion, 0, RoundingMode.DOWN).intValue();
                int cantidadUsada = Math.min(cantidadRequerida, cantidadDisponible);

                if (cantidadUsada > 0) {
                    // Crear el detalle y agregarlo a la colecci�n
                    DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                    detalle.setCaja(caja);
                    detalle.setCantidad(cantidadUsada);
                    detalle.setTotal(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                    nuevosDetalles.add(detalle);

                    // Actualizar monto restante
                    montoRestante = montoRestante.subtract(valorDenominacion.multiply(BigDecimal.valueOf(cantidadUsada)));
                    System.out.println(">>> Detalle generado: Caja=" + caja.getDenominacion().getValor() +
                                       ", Cantidad=" + cantidadUsada + ", Total=" + detalle.getTotal());

                    // A�adir al descriptionBuilder
                    descriptionBuilder.append("Cantidad ")
                        .append(cantidadUsada)
                        .append(" de billetes de valor ")
                        .append(valorDenominacion)
                        .append("\n");
                }
            }

            // Verificar si se cubri� el monto total
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                addWarning("No se pudo cubrir el monto total requerido. Monto restante: " + montoRestante);
                descriptionBuilder.append("No se pudo cubrir el monto total requerido. Monto restante: ")
                    .append(montoRestante)
                    .append("\n");
            }

            // Asignar la descripci�n generada (reemplaza cualquier texto previo)
            String nuevaDescripcion = descriptionBuilder.toString();
            cajaSalida.setDescripcion(nuevaDescripcion);
            getView().setValue("descripcion", nuevaDescripcion); // Actualizar solo el campo "descripcion"

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

            // Asignar la colecci�n convertida a la vista
            getView().setValue("detalle", detallesMap);
            System.out.println(">>> La colecci�n de detalles fue asignada a la vista.");

            // Refrescar �nicamente la colecci�n
            getView().refreshCollections();

            // Mensaje de �xito
            addMessage("La colecci�n de detalles ha sido completada correctamente.");

        } catch (Exception e) {
            // Capturar y mostrar cualquier error
            System.err.println(">>> Error en CajaSalidaGenerarDetalleAction: " + e.getMessage());
            e.printStackTrace();
            addError("Se produjo un error al completar la colecci�n de detalles. Consulte la consola para m�s detalles.");
        }
    }


    /**
     * Obtener las cajas disponibles seg�n la estrategia de pago seleccionada.
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
