package com.sta.cashtill.acciones;

import java.math.*;
import java.text.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeCajaSalidaAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad principal
        CajaSalida cajaSalida = (CajaSalida) getView().getEntity();

        if (cajaSalida == null) {
            addError("No se encontró la entidad CajaSalida.");
            return;
        }

        // Validar que el total de los detalles coincida con el importe del pago
        if (!validarTotalDetallesConImporte(cajaSalida)) {
            return; // Detener la ejecución si la validación falla
        }

        // Validar cantidades antes de ejecutar el movimiento
        if (!validarCajas(cajaSalida)) {
            addError("No hay suficiente cantidad en una o más cajas para realizar el movimiento.");
            return;
        }

        // Generar descripción del movimiento
        String descripcionGenerada = generarDescripcion(cajaSalida);
        getView().setValue("descripcion", descripcionGenerada);

        // Intentar guardar la entidad principal
        try {
            super.execute();
        } catch (Exception e) {
            addError("Error durante la ejecución de la acción: " + e.getMessage());
            e.printStackTrace();
            return; // Detener si ocurre un error
        }

        // Procesar los detalles solo si no hubo errores
        if (getErrors().isEmpty()) {
            for (DetalleCajaRegistradora detalle : cajaSalida.getDetalle()) {
                if (detalle == null) {
                    addError("El detalle es nulo. Revisa la configuración.");
                    continue;
                }

                // Validar y buscar la caja
                Caja caja = buscarCajaPorId(detalle.getCaja().getId());
                if (caja == null) {
                    addError("No se encontró una caja con el ID: " + detalle.getCaja().getId());
                    continue;
                }

                // Validar valores actuales antes de restar
                if (caja.getCantidad() == 0) {
                    caja.setCantidad(0); // Inicializar si es null
                }
                if (caja.getTotal() == null) {
                    caja.setTotal(BigDecimal.ZERO); // Inicializar si es null
                }

                // Actualizar los valores de la caja
                int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
                BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

                if (cantidad > caja.getCantidad()) {
                    addError("La cantidad en la caja con ID " + caja.getId() + " es insuficiente.");
                    continue;
                }

                caja.setCantidad(caja.getCantidad() - cantidad);
                caja.setTotal(caja.getTotal().subtract(total));

                // Persistir los cambios
                try {
                    XPersistence.getManager().merge(caja);
                } catch (Exception e) {
                    addError("Error al actualizar la caja con ID " + caja.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Cerrar diálogo y mostrar mensaje de éxito si no hubo errores
            if (getErrors().isEmpty()) {
                closeDialog();
                addMessage("Caja de salida actualizada correctamente.");
            } else {
                addError("Hubo errores durante la actualización de las cajas.");
            }
        }
    }

    /**
     * Valida que el total de los detalles coincida con el importe del pago y que haya suficiente dinero en las cajas.
     *
     * @param cajaSalida Entidad principal con los detalles a validar.
     * @return true si las validaciones se cumplen, false en caso contrario.
     */
    private boolean validarTotalDetallesConImporte(CajaSalida cajaSalida) {
        BigDecimal totalDetalles = cajaSalida.getDetalle().stream()
                .filter(Objects::nonNull)
                .map(detalle -> Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal importePago = Optional.ofNullable(cajaSalida.getImporte()).orElse(BigDecimal.ZERO);

        // Validar que el total de los detalles sea igual al importe del pago
        if (totalDetalles.compareTo(importePago) != 0) {
            addError("El total de los detalles no coincide con el importe del pago.");
            return false;
        }

        // Validar que haya suficiente dinero en las cajas para cubrir el importe
        BigDecimal totalCaja = obtenerTotalCaja();
        if (totalCaja.compareTo(importePago) < 0) {
            addError("No hay suficiente dinero en la caja para cubrir el importe del pago.");
            return false;
        }

        return true;
    }

    /**
     * Obtiene el total disponible en todas las cajas.
     *
     * @return El total disponible en las cajas.
     */
    private BigDecimal obtenerTotalCaja() {
        try {
            return XPersistence.getManager()
                    .createQuery("SELECT SUM(c.total) FROM Caja c", BigDecimal.class)
                    .getSingleResult();
        } catch (Exception e) {
            System.err.println("Error al calcular el total de las cajas: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    
    /**
     * Valida que las cajas tengan suficiente cantidad antes de realizar el movimiento.
     *
     * @param cajaSalida Entidad principal con los detalles a validar.
     * @return true si todas las cajas tienen suficiente cantidad, false en caso contrario.
     */
    private boolean validarCajas(CajaSalida cajaSalida) {
        for (DetalleCajaRegistradora detalle : cajaSalida.getDetalle()) {
            if (detalle == null) continue;

            Caja caja = buscarCajaPorId(detalle.getCaja().getId());
            if (caja == null || caja.getCantidad() == 0 || caja.getCantidad() < detalle.getCantidad()) {
                addError("La caja con ID " + detalle.getCaja().getId() + " no tiene suficiente cantidad.");
                return false;
            }
        }
        return true;
    }

    /**
     * Genera una descripción automática basada en los detalles de la CajaSalida.
     *
     * @param cajaSalida Instancia de CajaSalida.
     * @return La descripción generada.
     */
    private String generarDescripcion(CajaSalida cajaSalida) {
        // Obtener la descripción actual si existe
        String descripcionActual = Optional.ofNullable(cajaSalida.getDescripcion()).orElse("");

        // Obtener el usuario actual y la fecha
        String usuario = org.openxava.util.Users.getCurrent();
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        StringBuilder detalleDescripcion = new StringBuilder(
            String.format("\nDetalle de billetes retirados por %s el %s:\n", usuario, fecha)
        );

        // Definir el orden esperado de los IDs de caja
        List<String> ordenDenominaciones = Arrays.asList(
            "$50Mil.-", "$20Mil.-", "$10Mil.-", "$5Mil.-", "$2Mil.-", "$1Mil.-", "$500.-", "$200.-", "$100.-", "$50.-", "$20.-", "$10.-", "$5.-"
        );

        // Unificar los detalles con el mismo ID de caja en una estructura temporal
        Map<String, DetalleCajaRegistradora> detalleUnificado = new HashMap<>();
        for (DetalleCajaRegistradora detalle : cajaSalida.getDetalle()) {
            String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getId).orElse("N/A");
            DetalleCajaRegistradora existente = detalleUnificado.get(idCaja);

            if (existente == null) {
                // Crear un nuevo detalle unificado en la estructura temporal
                DetalleCajaRegistradora nuevoDetalle = new DetalleCajaRegistradora();
                nuevoDetalle.setCaja(detalle.getCaja());
                nuevoDetalle.setCantidad(detalle.getCantidad());
                nuevoDetalle.setTotal(detalle.getTotal());
                detalleUnificado.put(idCaja, nuevoDetalle);
            } else {
                // Sumar cantidades y totales al detalle existente en la estructura temporal
                existente.setCantidad(existente.getCantidad() + detalle.getCantidad());
                existente.setTotal(existente.getTotal().add(detalle.getTotal()));
            }
        }

        // Ordenar los detalles unificados según el orden de denominaciones
        List<DetalleCajaRegistradora> detallesOrdenados = new ArrayList<>(detalleUnificado.values());
        detallesOrdenados.sort((d1, d2) -> {
            String idCaja1 = Optional.ofNullable(d1.getCaja()).map(Caja::getId).orElse("N/A");
            String idCaja2 = Optional.ofNullable(d2.getCaja()).map(Caja::getId).orElse("N/A");
            int index1 = ordenDenominaciones.indexOf(idCaja1);
            int index2 = ordenDenominaciones.indexOf(idCaja2);
            return Integer.compare(index2, index1); // Ordenar de mayor a menor
        });

        // Construir el detalle de la descripción en columnas
        detalleDescripcion.append(String.format("%-20s%-15s%-15s\n", "Billete", "Cantidad", "Total"));
        detalleDescripcion.append("---------------------------------------------------\n");
        for (DetalleCajaRegistradora detalle : detallesOrdenados) {
            String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getId).orElse("N/A");
            int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
            BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

            detalleDescripcion.append(String.format(
                "%-20s%-15d$%-14s\n",
                idCaja, cantidad, total.toPlainString()
            ));
        }

        // Combinar la descripción actual con la nueva descripción generada
        return descripcionActual + detalleDescripcion.toString();
    }

    /**
     * Busca una caja en la base de datos utilizando su ID.
     *
     * @param cajaId ID de la caja a buscar.
     * @return Instancia de Caja encontrada o null si no existe.
     */
    private Caja buscarCajaPorId(String cajaId) {
        try {
            return XPersistence.getManager()
                    .createQuery("SELECT c FROM Caja c WHERE c.id = :cajaId", Caja.class)
                    .setParameter("cajaId", cajaId)
                    .getSingleResult();
        } catch (Exception e) {
            System.err.println("Error al buscar la caja con ID: " + cajaId + ". Excepción: " + e.getMessage());
            return null;
        }
    }
}

