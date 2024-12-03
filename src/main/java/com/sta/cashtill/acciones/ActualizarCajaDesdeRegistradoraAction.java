package com.sta.cashtill.acciones;

import java.math.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.auxiliares.*;
import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeRegistradoraAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad CajaRegistradora desde la vista
        CajaRegistradora cajaRegistradora = (CajaRegistradora) getView().getEntity();

        if (cajaRegistradora == null) {
            addError("No se encontró la entidad CajaRegistradora.");
            System.err.println("Error: Entidad CajaRegistradora no encontrada.");
            return;
        }

        // Determinar el tipo de movimiento
        TipoMovimiento tipoMovimiento = determinarTipoMovimiento(cajaRegistradora);
        if (tipoMovimiento == null) {
            addError("No se pudo determinar el tipo de movimiento.");
            System.err.println("Error: Tipo de movimiento no determinado.");
            return;
        }

        // Validar todas las cajas antes de realizar actualizaciones
        if (!validarCajas(cajaRegistradora, tipoMovimiento)) {
            addError("Uno o más tipos de billetes no tienen suficiente cantidad para realizar el movimiento: " + cajaRegistradora.getMovimientoCajaNombre());
            System.err.println("Error: Validación fallida para el movimiento.");
            return; // Salir sin ejecutar el super.execute()
        }

        // Guardar la entidad principal
        super.execute();

        // Procesar cada detalle de la caja registradora
        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            if (detalle == null) {
                addError("El detalle es nulo y no puede procesarse.");
                System.err.println("Advertencia: Detalle nulo en la lista de detalles.");
                continue;
            }

            String cajaId = detalle.getCaja().getId();
            if (cajaId == null || cajaId.isEmpty()) {
                addError("El ID de la caja en el detalle es nulo o vacío: " + detalle);
                System.err.println("Advertencia: ID de caja vacío en detalle: " + detalle);
                continue;
            }

            // Buscar la caja asociada por su ID
            Caja caja = buscarCajaPorId(cajaId);
            if (caja == null) {
                addError("No se encontró una caja con el ID: " + cajaId);
                System.err.println("Advertencia: Caja no encontrada con ID: " + cajaId);
                continue;
            }

            // Actualizar los valores de la caja
            actualizarValoresCaja(caja, detalle, tipoMovimiento);

            // Persistir los cambios en la caja
            try {
                XPersistence.getManager().merge(caja);
                System.out.println("Caja actualizada exitosamente: " + caja);
            } catch (Exception e) {
                addError("Error al actualizar la caja con ID " + cajaId + ": " + e.getMessage());
                System.err.println("Error: No se pudo actualizar la caja con ID " + cajaId + ". Excepción: " + e.getMessage());
            }
        }

        // Cerrar diálogo y mostrar mensaje de éxito
        closeDialog();
        addMessage("Caja actualizada correctamente.");
        System.out.println("Movimiento procesado con éxito para CajaRegistradora: " + cajaRegistradora.getId());
    }

    // Valida si todas las cajas tienen suficiente cantidad antes de realizar actualizaciones
    private boolean validarCajas(CajaRegistradora cajaRegistradora, TipoMovimiento tipoMovimiento) {
        if (tipoMovimiento == TipoMovimiento.ENTRADA) {
            return true; // No es necesario validar para entradas
        }

        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            if (detalle == null) continue;

            String cajaId = detalle.getCaja().getId();
            if (cajaId == null || cajaId.isEmpty()) continue;

            // Buscar la caja asociada por su ID
            Caja caja = buscarCajaPorId(cajaId);
            if (caja == null) continue;

            // Validar cantidad
            if (caja.getCantidad() < detalle.getCantidad()) {
                addError("Los billetes de la denominación " + obtenerDenominacionCaja(caja) +
                         " no son suficientes. Cantidad disponible: " + caja.getCantidad() +
                         ", cantidad requerida: " + detalle.getCantidad());
                System.err.println("Validación fallida para caja: " + cajaId + ", denominación: " + obtenerDenominacionCaja(caja));
                return false; // Detener la validación si una caja no cumple
            }
        }

        return true; // Todas las cajas tienen suficiente cantidad
    }

    // Obtiene la denominación de la caja
    private String obtenerDenominacionCaja(Caja caja) {
        return Optional.ofNullable(caja.getDenominacion())
                .map(Denominaciones::getNombre)
                .orElse("desconocida");
    }

    // Determina el tipo de movimiento según la clase hija de CajaRegistradora.
    private TipoMovimiento determinarTipoMovimiento(CajaRegistradora cajaRegistradora) {
        if (cajaRegistradora instanceof CajaEntrada) {
            return TipoMovimiento.ENTRADA;
        } else if (cajaRegistradora instanceof CajaSalida) {
            return TipoMovimiento.SALIDA;
        }
        return null;
    }

    // Actualiza los valores de la caja según el tipo de movimiento.
    private void actualizarValoresCaja(Caja caja, DetalleCajaRegistradora detalle, TipoMovimiento tipoMovimiento) {
        int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
        BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

        if (tipoMovimiento == TipoMovimiento.ENTRADA) {
            caja.setCantidad(caja.getCantidad() + cantidad);
            caja.setTotal(caja.getTotal().add(total));
        } else if (tipoMovimiento == TipoMovimiento.SALIDA) {
            caja.setCantidad(caja.getCantidad() - cantidad);
            caja.setTotal(caja.getTotal().subtract(total));
        }

        caja.getCalculaTotal();
        System.out.println("Valores actualizados para Caja ID: " + caja.getId() + ", Cantidad: " + caja.getCantidad() + ", Total: " + caja.getTotal());
    }

    // Busca una instancia de Caja por su ID.
    private Caja buscarCajaPorId(String cajaId) {
        try {
            return XPersistence.getManager()
                    .createQuery("SELECT c FROM Caja c WHERE c.id = :cajaId", Caja.class)
                    .setParameter("cajaId", cajaId)
                    .getSingleResult();
        } catch (Exception e) {
            System.err.println("Error: No se encontró Caja con ID: " + cajaId + ". Excepción: " + e.getMessage());
            return null;
        }
    }
}
