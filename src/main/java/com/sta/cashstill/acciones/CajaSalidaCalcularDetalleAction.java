package com.sta.cashstill.acciones;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.util.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.modelo.*;


/**
 * Ejemplo completo de acción en OpenXava que realiza el cálculo
 * de billetes/monedas a entregar, usando un enfoque de
 * Programación Dinámica (bounded coin change) para cubrir un importe exacto.
 */
public class CajaSalidaCalcularDetalleAction extends ViewBaseAction {

    @SuppressWarnings("unchecked")
	@Override
    public void execute() throws Exception {
        try {
            // 1) Obtener la entidad CajaSalida desde la vista
            CajaSalida cajaSalida = (CajaSalida) getView().getEntity();
            BigDecimal importe = cajaSalida.getImporte(); // Valor entero en BigDecimal

            // 2) Calcular total disponible
            BigDecimal totalDisponible = calcularTotalDisponible();

            // 3) Verificar si hay suficiente dinero
            if (importe.compareTo(totalDisponible) > 0) {
                addError("El total disponible en la caja no es suficiente para cubrir el pago. "
                    + "Disponible: " + totalDisponible
                    + ", Requerido: " + importe);
                return;
            }

            // 4) Obtener la lista de cajas (denominaciones) disponibles
            List<Caja> cajas = obtenerCajasOrdenadas();

            // 5) Aplicar estrategia DP para lograr importe exacto
            List<DetalleCajaRegistradora> nuevosDetalles = aplicarEstrategiaDP(importe, cajas);

            if (nuevosDetalles == null || nuevosDetalles.isEmpty()) {
                // No hay solución para cubrir importe exacto
                addError("No es posible cubrir el importe exacto con las denominaciones disponibles.");
                return;
            }

            // 6) Unificar y ordenar detalles (por si alguna vez hubiera duplicados de la misma denominación)
            nuevosDetalles = unificarDetalles(nuevosDetalles);
            // Ordenar de mayor a menor denominación
            nuevosDetalles.sort(
                Comparator.comparing((DetalleCajaRegistradora d) -> d.getCaja().getValor()).reversed()
            );

            // 7) Convertir cada DetalleCajaRegistradora a un Map y asignar a la vista
            List<Map<String, Object>> detallesMap = new ArrayList<>();
            for (DetalleCajaRegistradora detalle : nuevosDetalles) {
                if (detalle == null 
                        || detalle.getCaja() == null 
                        || detalle.getTotal() == null
                        || detalle.getCantidad() <= 0) {
                    addError("Uno de los detalles está incompleto o contiene datos nulos.");
                    return;
                }
                Map<String, Object> detalleMap = new HashMap<>();
                detalleMap.put("caja.id", detalle.getCaja().getId());
                detalleMap.put("cantidad", detalle.getCantidad());
                detalleMap.put("total", detalle.getTotal());
                // Convierte el mapa a un árbol para que OpenXava lo interprete correctamente
                detalleMap = Maps.plainToTree(detalleMap);
                detallesMap.add(detalleMap);
            }

            // Asignar la colección a la vista
            getView().setValue("detalle", detallesMap);
            getView().refreshCollections();

            // 8) Mensaje de éxito
            addMessage("Cálculo realizado correctamente (DP).");

        } catch (Exception e) {
            addError("Se produjo un error al completar la colección de detalles (DP): " + e.getMessage());
        }
    }

    /**
     * Algoritmo de Programación Dinámica para "bounded coin change" con denominaciones limitadas.
     * @param importe Monto a cubrir (BigDecimal, pero se asume entero).
     * @param cajas Lista de objetos Caja con valor (denominación) y cantidad.
     * @return Lista de DetalleCajaRegistradora que cubre exactamente el importe, o null si no hay solución.
     */
    private List<DetalleCajaRegistradora> aplicarEstrategiaDP(BigDecimal importe, List<Caja> cajas) {
        // Convertir BigDecimal a int (se asume que ambos son valores enteros)
        int T = importe.intValueExact();

        int n = cajas.size();
        // Arrays para almacenar las denominaciones y cantidades
        int[] denom = new int[n];
        int[] qty   = new int[n];

        for (int i = 0; i < n; i++) {
            // Se asume que getValor() retorna un BigDecimal con un valor entero (50, 100, 500, etc.)
            denom[i] = cajas.get(i).getValor().intValue();
            qty[i]   = cajas.get(i).getCantidad();
        }

        // DP bidimensional: dp[i][j] = # mínimo de billetes para llegar a j usando las primeras i denominaciones
        final int INF = Integer.MAX_VALUE / 2; 
        int[][] dp = new int[n+1][T+1]; 
        // "choice" para reconstruir la solución
        int[][] choice = new int[n+1][T+1];

        // Inicializar la DP con infinito
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dp[i], INF);
        }
        dp[0][0] = 0; // 0 billetes para llegar a 0 con 0 denominaciones

        // Llenar la tabla DP
        for (int i = 1; i <= n; i++) {
            int valor = denom[i-1]; // denom de la i-ésima caja (índice en arrays = i-1)
            int maxQty = qty[i-1];

            for (int sum = 0; sum <= T; sum++) {
                // Por defecto, heredamos la mejor solución de i-1 denominaciones
                dp[i][sum] = dp[i-1][sum];
                choice[i][sum] = 0; // no usar esta denominación

                // Intentar usar k billetes de la i-ésima denominación
                for (int k = 1; k <= maxQty; k++) {
                    int prevSum = sum - k * valor;
                    if (prevSum < 0) break; // ya no es válido

                    if (dp[i-1][prevSum] != INF) {
                        int candidato = dp[i-1][prevSum] + k;
                        if (candidato < dp[i][sum]) {
                            dp[i][sum] = candidato;
                            choice[i][sum] = k; // usar k billetes de esta denom
                        }
                    }
                }
            }
        }

        // Si dp[n][T] sigue siendo INF, no hay forma de cubrir el importe exacto
        if (dp[n][T] >= INF) {
            return null;
        }

        // Reconstruir la solución a partir de "choice"
        List<DetalleCajaRegistradora> detalles = new ArrayList<>();
        int montoPendiente = T;
        for (int i = n; i >= 1; i--) {
            int k = choice[i][montoPendiente];
            if (k > 0) {
                // Se usaron k billetes de la i-ésima caja
                Caja caja = cajas.get(i-1);
                DetalleCajaRegistradora detalle = new DetalleCajaRegistradora();
                detalle.setCaja(caja);
                detalle.setCantidad(k);

                // Valor total (caja.getValor() es BigDecimal)
                BigDecimal totalDetalle = caja.getValor().multiply(BigDecimal.valueOf(k));
                detalle.setTotal(totalDetalle);

                detalles.add(detalle);

                // Ajustar el monto pendiente
                montoPendiente -= (k * denom[i-1]);
            }
        }

        // montoPendiente debería quedar en 0 si todo salió bien
        return detalles;
    }

    /**
     * Unificar detalles por denominación (por si vinieran repetidos).
     */
    private List<DetalleCajaRegistradora> unificarDetalles(List<DetalleCajaRegistradora> detalles) {
        Map<Caja, DetalleCajaRegistradora> map = new HashMap<>();

        for (DetalleCajaRegistradora d : detalles) {
            Caja caja = d.getCaja();
            if (map.containsKey(caja)) {
                DetalleCajaRegistradora existente = map.get(caja);
                existente.setCantidad(existente.getCantidad() + d.getCantidad());
                existente.setTotal(existente.getTotal().add(d.getTotal()));
            } else {
                map.put(caja, d);
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Calcular el total de dinero disponible en las cajas (todas las denominaciones).
     */
    private BigDecimal calcularTotalDisponible() {
        String query = "SELECT SUM(c.denominacion.valor * c.cantidad) FROM Caja c WHERE c.cantidad > 0";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        BigDecimal totalDisponible = (BigDecimal) manager.createQuery(query).getSingleResult();
        return totalDisponible != null ? totalDisponible : BigDecimal.ZERO;
    }

    /**
     * Obtener las cajas disponibles, ordenadas de mayor a menor denominación.
     */
    private List<Caja> obtenerCajasOrdenadas() {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0 ORDER BY c.denominacion.valor DESC";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        return manager.createQuery(query, Caja.class).getResultList();
    }
}
