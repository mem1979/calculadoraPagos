package com.sta.cashtill.acciones;

import java.math.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;


public class ActualizarCajaDesdeRegistradoraAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad CajaRegistradora desde la vista
        CajaRegistradora cajaRegistradora = (CajaRegistradora) getView().getEntity();

        if (cajaRegistradora == null) {
            addError("No se encontró la entidad CajaRegistradora.");
            return;
        }

        // Determinar el tipo de movimiento
        TipoMovimiento tipoMovimiento = determinarTipoMovimiento(cajaRegistradora);
        if (tipoMovimiento == null) {
            addError("No se pudo determinar el tipo de movimiento.");
            return;
        }

        // Validar todas las cajas antes de realizar actualizaciones
        if (!validarCajas(cajaRegistradora, tipoMovimiento)) {
            addError("Uno o más tipo de Billetes, no tienen suficiente cantidad para realizar " + cajaRegistradora.getMovimientoCajaNombre());
            return; // Salir sin ejecutar el super.execute()
        }

        
        // Llamar al método de guardado de SaveAction
        super.execute();

        // Iterar sobre los detalles y actualizar las cajas asociadas
        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            if (detalle == null) {
                addError("El detalle es nulo y no puede procesarse.");
                continue;
            }
            //   String cajaId = detalle.getCaja().getId(); // Obtener el ID de la caja desde el detalle
            String cajaId = detalle.getCajaId();
            if (cajaId == null || cajaId.isEmpty()) {
                addError("El ID de la caja en el detalle es nulo o vacío: " + detalle);
                continue;
            }
      

            // Buscar la caja asociada por su ID
            Caja caja = buscarCajaPorId(cajaId);
            if (caja == null) {
                addError("No se encontró una caja con el ID: " + cajaId);
                continue;
            }

            // Actualizar los valores según el movimiento
            actualizarValoresCaja(caja, detalle, tipoMovimiento);

            // Persistir los cambios en la caja
            try {
                XPersistence.getManager().merge(caja);
            } catch (Exception e) {
                addError("Error al actualizar la caja con ID " + cajaId + ": " + e.getMessage());
            }
        }

        // Cerrar Dialog(CajaEntrada, CajaSalida)
        closeDialog();
        addMessage("Caja actualizada correctamente.");
    }

    // Valida si todas las cajas tienen suficiente cantidad antes de realizar actualizaciones
    private boolean validarCajas(CajaRegistradora cajaRegistradora, TipoMovimiento tipoMovimiento) {
        if (tipoMovimiento == TipoMovimiento.ENTRADA) {
            return true; // No es necesario validar para entradas
        }

        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            if (detalle == null) {
                continue;
            }

            String cajaId = (detalle.getCaja() != null && detalle.getCaja().getId() != null && !detalle.getCaja().getId().isEmpty()) 
                    ? detalle.getCaja().getId() 
                    : detalle.getCajaId();

    if (cajaId == null || cajaId.isEmpty()) {
        continue;
    }

            // Buscar la caja asociada por su ID
            Caja caja = buscarCajaPorId(cajaId);
            if (caja == null) {
                continue;
            }

            // Validar cantidad
            if (caja.getCantidad() < detalle.getCantidad()) {
                addError("Los billetes de la denominación " + obtenerDenominacionCaja(caja) +
                         " no son suficientes. Cantidad disponible: " + caja.getCantidad() +
                         ", cantidad requerida: " + detalle.getCantidad());
                return false; // Detener la validación si una caja no cumple
            }
        }

        return true; // Todas las cajas tienen suficiente cantidad
    }

    // Obtiene la denominación de la caja
    private String obtenerDenominacionCaja(Caja caja) {
        // Reemplaza este método si la denominación está en otra entidad o calculada
        return caja.getDenominacion().getNombre() != null ? caja.getDenominacion().getNombre() : "desconocida";
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
        int cantidad = detalle.getCantidad() == 0 ? 0 : detalle.getCantidad();
        BigDecimal total = detalle.getTotal() == null ? BigDecimal.ZERO : detalle.getTotal();

        if (tipoMovimiento == TipoMovimiento.ENTRADA) {
            // Sumar valores
            caja.setCantidad((caja.getCantidad() == 0 ? 0 : caja.getCantidad()) + cantidad);
            caja.setTotal((caja.getTotal() == null ? BigDecimal.ZERO : caja.getTotal()).add(total));
        } else if (tipoMovimiento == TipoMovimiento.SALIDA) {
            // Restar valores
            caja.setCantidad((caja.getCantidad() == 0 ? 0 : caja.getCantidad()) - cantidad);
            caja.setTotal((caja.getTotal() == null ? BigDecimal.ZERO : caja.getTotal()).subtract(total));
        }

        // Recalcular el total basado en los nuevos valores
        caja.getCalculaTotal();
    }

    // Busca una instancia de Caja por su ID.
    private Caja buscarCajaPorId(String cajaId) {
        try {
            return (Caja) XPersistence.getManager()
                .createQuery("SELECT c FROM Caja c WHERE c.id = :cajaId")
                .setParameter("cajaId", cajaId)
                .getSingleResult();
        } catch (Exception e) {
            return null; // Manejar el caso en que no se encuentre la caja
        }
    }
}
