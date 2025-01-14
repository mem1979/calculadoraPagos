package com.sta.cashstill.acciones;

import java.math.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.util.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.modelo.*;


/**
 * Ejemplo de Greedy Híbrido / Multi-pasadas + Ajustes:
 *
 * 1ra pasada: 
 *   - Ordenar por mayor (valor * cantidad).
 *   - Tomar la máxima cantidad posible de cada denominación (greedy típico).
 *
 * 2da pasada:
 *   - Ajustar el resto que quede, por ejemplo usando denominaciones más pequeñas o 
 *     "devolviendo" parte de las asignaciones grandes si con su fracción en denominaciones 
 *     más pequeñas se logra un mejor ajuste.
 *
 * 3ra pasada (opcional):
 *   - Intercambios más complejos (si aún no se cubre).
 *
 * Todo sin modificar la base de datos (usa copia local de cantidades).
 */
public class CajaSalidaCalcularDetalleAction extends ViewBaseAction {

    @SuppressWarnings("unchecked")
	@Override
    public void execute() throws Exception {
        try {
            // 1) Obtener la entidad CajaSalida
            CajaSalida cajaSalida = (CajaSalida) getView().getEntity();
            BigDecimal importe = cajaSalida.getImporte(); // BigDecimal, entero lógico

            // 2) Calcular el total disponible
            BigDecimal totalDisponible = calcularTotalDisponible();

            // 3) Verificar si hay suficiente en caja
            if (importe.compareTo(totalDisponible) > 0) {
                addError("El total disponible en la caja no es suficiente para cubrir el pago. "
                    + "Disponible: " + totalDisponible
                    + ", Requerido: " + importe);
                return;
            }

            // 4) Obtener cajas con cantidad > 0
            List<Caja> cajasDisponibles = obtenerCajasDisponibles();

            // 5) Crear copia local de cantidades
            Map<Caja, Integer> copiaCantidades = new HashMap<>();
            for (Caja c : cajasDisponibles) {
                copiaCantidades.put(c, c.getCantidad());
            }

            // 6) Lista donde acumulamos la asignación (detalle)
            List<DetalleCajaRegistradora> detalleFinal = new ArrayList<>();

            // ------------------------------------------------------------------
            // PRIMERA PASADA:
            // Ordenar por mayor (valor * cantidad) y asignar greedily
            // ------------------------------------------------------------------
            cajasDisponibles.sort((c1, c2) -> {
                BigDecimal total1 = c1.getValor().multiply(BigDecimal.valueOf(copiaCantidades.get(c1)));
                BigDecimal total2 = c2.getValor().multiply(BigDecimal.valueOf(copiaCantidades.get(c2)));
                return total2.compareTo(total1); // desc
            });

            BigDecimal montoRestante = importe;
            aplicarGreedy(montoRestante, cajasDisponibles, copiaCantidades, detalleFinal);

            BigDecimal usado = sumarTotal(detalleFinal);
            montoRestante = importe.subtract(usado);

            // ------------------------------------------------------------------
            // SEGUNDA PASADA (si queda resto):
            // Ajustar el resto usando billetes/monedas más pequeños
            // o "devolviendo" parte de la asignación grande
            // ------------------------------------------------------------------
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                // Ordenar ahora por valor ASC (p.ej.) para probar con denominaciones pequeñas
                // o reusar la misma lista pero en orden asc
                cajasDisponibles.sort(Comparator.comparing(Caja::getValor));

                // Aquí podemos intentar:
                //  1) AplicarGreedy con la lista asc, 
                //     ver si ayuda a cubrir algo del resto.
                //  2) O intentar "devolver" uno de los billetes grandes asignados y reemplazar 
                //     con varios más pequeños.

                // Ejemplo simple: aplicar Greedy nuevamente en orden asc, 
                //                sin "devolver" billetes:
                aplicarGreedy(montoRestante, cajasDisponibles, copiaCantidades, detalleFinal);

                usado = sumarTotal(detalleFinal);
                montoRestante = importe.subtract(usado);
            }

            // ------------------------------------------------------------------
            // TERCERA PASADA (OPCIONAL):
            // Intercambios más complejos: 
            //  p. ej., si falta 50 y no hay 50 disponible,
            //  devolver un billete de 100 y usar 2x50, etc.
            // ------------------------------------------------------------------
            if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
                // Ejemplo muy simplificado de "intercambio":
                // Exploramos si podemos "soltar" 1 billete grande (o varios) y 
                // reemplazarlo(s) por billetes/monedas más pequeños(as).
                // Nota: el siguiente es un pseudo-código rápido:
                boolean exitoIntercambio = intentarIntercambios(detalleFinal, cajasDisponibles, copiaCantidades, montoRestante, importe);

                if (!exitoIntercambio) {
                    // Si sigue en > 0, no se pudo cubrir
                    BigDecimal faltante = importe.subtract(sumarTotal(detalleFinal));
                    if (faltante.compareTo(BigDecimal.ZERO) > 0) {
                        addError("No se pudo cubrir el importe exacto tras 3 pasadas. Faltante: " + faltante);
                        return;
                    }
                }
                // Si el intercambio funcionó y cubrimos, perfecto.
            }

            // Verificar una última vez
            BigDecimal totalAsignado = sumarTotal(detalleFinal);
            BigDecimal faltanteFinal = importe.subtract(totalAsignado);
            if (faltanteFinal.compareTo(BigDecimal.ZERO) > 0) {
                addError("No se pudo cubrir el importe exacto. Faltante: " + faltanteFinal);
                return;
            }

            // ------------------------------------------------------------------
            // Si llegamos aquí, se cubrió el importe
            // Unificar y ordenar detalles
            // ------------------------------------------------------------------
            detalleFinal = unificarDetalles(detalleFinal);
            // Orden final por denominación desc (ejemplo)
            detalleFinal.sort(
                Comparator.comparing((DetalleCajaRegistradora d) -> d.getCaja().getValor()).reversed()
            );

            // Convertir a Map y asignar a la vista
            List<Map<String, Object>> detallesMap = new ArrayList<>();
            for (DetalleCajaRegistradora d : detalleFinal) {
                if (d.getCantidad() <= 0 || d.getTotal() == null || d.getCaja() == null) {
                    addError("Uno de los detalles está incompleto o contiene datos nulos.");
                    return;
                }
                Map<String, Object> mapDet = new HashMap<>();
                mapDet.put("caja.id", d.getCaja().getId());
                mapDet.put("cantidad", d.getCantidad());
                mapDet.put("total", d.getTotal());
                mapDet = Maps.plainToTree(mapDet);
                detallesMap.add(mapDet);
            }

            getView().setValue("detalle", detallesMap);
            getView().refreshCollections();

            addMessage("Greedy multi-pasadas aplicado con éxito.");

        } catch (Exception e) {
            addError("Error en cálculo multi-pasadas: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    // MÉTODOS PRINCIPALES DE LAS PASADAS
    // --------------------------------------------------------------------------

    /**
     * Aplica una lógica Greedy simple: tomar la máxima cantidad posible
     * de cada denominación (en el orden que recibamos las cajas),
     * hasta agotar 'montoRestante' o 'copiaCantidades'.
     */
    private void aplicarGreedy(
            BigDecimal montoRestante,
            List<Caja> cajas,
            Map<Caja, Integer> copiaCantidades,
            List<DetalleCajaRegistradora> detalles
    ) {
        for (Caja caja : cajas) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) break;

            int cantDisponible = copiaCantidades.getOrDefault(caja, 0);
            if (cantDisponible <= 0) continue;

            BigDecimal valor = caja.getValor();

            // requerido = floor(montoRestante / valor)
            BigDecimal reqBD = montoRestante.divide(valor, 0, RoundingMode.DOWN);
            int requerido = reqBD.intValue();

            int usado = Math.min(requerido, cantDisponible);
            if (usado > 0) {
                DetalleCajaRegistradora d = new DetalleCajaRegistradora();
                d.setCaja(caja);
                d.setCantidad(usado);

                BigDecimal totalDet = valor.multiply(BigDecimal.valueOf(usado));
                d.setTotal(totalDet);

                detalles.add(d);

                montoRestante = montoRestante.subtract(totalDet);
                copiaCantidades.put(caja, cantDisponible - usado);
            }
        }
    }

    /**
     * Ejemplo simplificado de "tercera pasada" con intercambios:
     * Devuelve algún billete grande en 'detalles' e intenta sustituirlo 
     * por varios billetes más pequeños de 'cajas'.
     */
    private boolean intentarIntercambios(
            List<DetalleCajaRegistradora> detalles,
            List<Caja> cajas,
            Map<Caja, Integer> copiaCantidades,
            BigDecimal montoRestante,
            BigDecimal importe
    ) {
        // Un ejemplo básico:
        // 1) Buscar en 'detalles' si hay algún billete "grande" que podamos "devolver".
        // 2) Liberar su cantidad en el map local (copiaCantidades).
        // 3) Con ese "espacio", tratar de cubrir 'montoRestante' (y parte de lo que devolvimos)
        //    con billetes más pequeños.

        // Este es un ejemplo "naive":
        // - Intentar devolver 1 billete, y ver si con billetes más pequeños completamos.
        // Realmente se pueden hacer varios bucles y heurísticas.

        // Ordenar 'detalles' desc para buscar el más grande
        detalles.sort(Comparator.comparing((DetalleCajaRegistradora d) -> d.getCaja().getValor()).reversed());
        
        for (DetalleCajaRegistradora dt : detalles) {
            Caja cajaGrande = dt.getCaja();
            if (dt.getCantidad() <= 0) continue;

            // Intentar devolver "1" billete
            dt.setCantidad(dt.getCantidad() - 1);  
            BigDecimal valorGrande = cajaGrande.getValor();
            BigDecimal devuelto = valorGrande;  // 1 billete

            // Actualizar total en dt
            dt.setTotal(dt.getTotal().subtract(valorGrande));

            // Aumentar en copiaCantidades ese billete
            copiaCantidades.put(cajaGrande, copiaCantidades.getOrDefault(cajaGrande, 0) + 1);

            // Con ese "espacio" devuelto, 
            // sube el 'montoRestante' (porque estás "descontando" lo que ya habías cubierto)
            montoRestante = montoRestante.add(devuelto);

            // Intentar usar billetes más pequeños (orden asc, por ejemplo)
            cajas.sort(Comparator.comparing(Caja::getValor));
            aplicarGreedy(montoRestante, cajas, copiaCantidades, detalles);

            // Recalcular
            BigDecimal totalActual = sumarTotal(detalles);
            BigDecimal faltante = importe.subtract(totalActual);
            if (faltante.compareTo(BigDecimal.ZERO) <= 0) {
                // Pudimos cubrir al 100%
                return true;
            }
            else {
                // No se cubrió, revertir (dejar como estaba) e intentar con otro billete
                // revertimos la sustracción
                dt.setCantidad(dt.getCantidad() + 1);
                dt.setTotal(dt.getTotal().add(valorGrande));
                copiaCantidades.put(cajaGrande, copiaCantidades.get(cajaGrande) - 1);
                montoRestante = faltante;  // volvemos al faltante original
            }
        }
        return false;
    }

    // --------------------------------------------------------------------------
    // MÉTODOS AUXILIARES
    // --------------------------------------------------------------------------

    /**
     * Suma total de una lista de DetalleCajaRegistradora.
     */
    private BigDecimal sumarTotal(List<DetalleCajaRegistradora> detalles) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DetalleCajaRegistradora d : detalles) {
            if (d.getTotal() != null) {
                sum = sum.add(d.getTotal());
            }
        }
        return sum;
    }

    /**
     * Unifica los detalles por denominación.
     */
    private List<DetalleCajaRegistradora> unificarDetalles(List<DetalleCajaRegistradora> detalles) {
        Map<Caja, DetalleCajaRegistradora> map = new HashMap<>();
        for (DetalleCajaRegistradora d : detalles) {
            Caja c = d.getCaja();
            if (map.containsKey(c)) {
                DetalleCajaRegistradora ext = map.get(c);
                ext.setCantidad(ext.getCantidad() + d.getCantidad());
                ext.setTotal(ext.getTotal().add(d.getTotal()));
            } else {
                map.put(c, d);
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Calcula el total de dinero disponible.
     */
    private BigDecimal calcularTotalDisponible() {
        String query = "SELECT SUM(c.denominacion.valor * c.cantidad) FROM Caja c WHERE c.cantidad > 0";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        BigDecimal total = (BigDecimal) manager.createQuery(query).getSingleResult();
        return (total != null) ? total : BigDecimal.ZERO;
    }

    /**
     * Obtiene la lista de cajas con cantidad > 0.
     */
    private List<Caja> obtenerCajasDisponibles() {
        String query = "SELECT c FROM Caja c WHERE c.cantidad > 0";
        EntityManager manager = org.openxava.jpa.XPersistence.getManager();
        return manager.createQuery(query, Caja.class).getResultList();
    }
}
