package com.sta.cashtill.acciones;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

public class CajaRegistradoraActualizarDetalleCategoriaMovimientoAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        // Obtenemos el valor del campo `movimientoCaja`
        Object movimientoCajaId = getNewValue();

        // Verificamos si es nulo
        if (movimientoCajaId == null) {
            // Limpiar la descripción si no hay movimientoCaja seleccionado
            getView().setValue("descripcion", "");
        } else {
            // Buscar la descripción del movimientoCaja en la base de datos usando XPersistence
            String descripcionMovimientoCaja = buscarDescripcionMovimientoCaja(movimientoCajaId);

            // Asignar la descripción al campo `descripcion`
            if (descripcionMovimientoCaja != null) {
                getView().setValue("descripcion", descripcionMovimientoCaja);
            } else {
                addWarning("movimientoCaja_no_encontrado", movimientoCajaId);
            }
        }
    }

    /**
     * Busca la descripción del movimientoCaja en la base de datos.
     * 
     * @param movimientoCajaId El ID del movimientoCaja
     * @return La descripción del movimientoCaja, o null si no se encuentra
     */
    private String buscarDescripcionMovimientoCaja(Object movimientoCajaId) {
        try {
            // Crear la consulta para obtener la descripción
            Query query = XPersistence.getManager().createQuery(
                "SELECT m.descripcion FROM MovimientoCaja m WHERE m.id = :id"
            );
            query.setParameter("id", movimientoCajaId);

            // Retornar el resultado de la consulta
            return (String) query.getSingleResult();
        } catch (Exception e) {
            // Manejar el caso donde no se encuentre el movimientoCaja
            return null;
        }
    }
}
