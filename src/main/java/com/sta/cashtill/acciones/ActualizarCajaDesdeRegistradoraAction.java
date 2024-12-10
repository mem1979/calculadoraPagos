package com.sta.cashtill.acciones;

import java.math.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeRegistradoraAction  extends SaveAction {

	@Override
    public void execute() throws Exception {
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

        // Iterar sobre los detalles y actualizar las cajas asociadas
        for (DetalleCajaRegistradora detalle : cajaRegistradora.getDetalle()) {
            if (detalle == null) {
                addError("El detalle es nulo y no puede procesarse.");
                continue;
            }

            String cajaId = detalle.getCaja().getId(); // Obtener el ID de la caja desde el detalle
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

        addMessage("Cajas actualizadas correctamente.");
        super.execute();
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