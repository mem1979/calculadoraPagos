package com.sta.cashstill.acciones;

import java.math.*;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

public class NuevaCajaSalidaVueltoAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        try {
            // Intentar obtener el ID del movimiento "VUELTO"
            String MovimientoId = null;
            try {
                MovimientoId = (String) XPersistence.getManager()
                        .createQuery("SELECT c.id FROM MovimientoCaja c WHERE c.nombre = 'VUELTO'") // Consulta JPQL
                        .getSingleResult();
            } catch (NoResultException e) {
                // Si no existe el movimiento "VUELTO", informar al usuario
                addError("Debe crear un Movimiento de Caja de tipo Salida con el nombre 'VUELTO'");
                return;
            }

            // Obtener el valor del vuelto
            BigDecimal vuelto = (BigDecimal) getView().getValue("vuelto");
            //getView().setValueNotifying("conVuelto", true);

            // Configurar la vista para la salida de efectivo
            removeActions("CajaRegistradora.REGISTRAR VUELTO");
            showDialog();
            getView().setModelName("CajaSalida");
            getView().setViewName("salida");
            getView().setTitle("SALIDA DE EFECTIVO - VUELTO");

            // Asignar valores a la vista
            getView().setValueNotifying("movimientoCaja.id", MovimientoId); // Asignar el ID de "VUELTO"
            getView().setEditable("movimientoCaja", false);

            getView().setValueNotifying("conVuelto", true);
            getView().setValue("importe", vuelto);
            getView().setEditable("importe", false);

            // Agregar acciones
            addActions("MovimientoCaja.ActualizarCajaSalida", "Dialog.cancel");
        } catch (Exception ex) {
            ex.printStackTrace();
            addError("system_error");
        }
    }
}
