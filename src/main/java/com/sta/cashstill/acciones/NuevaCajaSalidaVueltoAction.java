package com.sta.cashstill.acciones;

import java.math.*;

import javax.persistence.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.enums.*;

public class NuevaCajaSalidaVueltoAction extends ViewBaseAction {

	  @Override
	    public void execute() throws Exception {
	        try {
	            // Elimina las acciones innecesarias
	            removeActions("CajaRegistradora.REGISTRAR VUELTO");

	            // Obtener o crear el movimiento 'VUELTO'
	            String movimientoId = obtenerOCrearMovimiento("VUELTO");

	            // Obtener el valor del campo "vuelto"
	            BigDecimal vuelto = (BigDecimal) getView().getValue("vuelto");

	            // Configurar la vista para la salida de efectivo
	            showDialog();
	            getView().setModelName("CajaSalida");
	            getView().setViewName("salida");
	            getView().setTitle("SALIDA DE EFECTIVO - VUELTO");

	            getView().setValueNotifying("movimientoCaja.id", movimientoId); // Asignar el ID de "VUELTO"
	            getView().setEditable("movimientoCaja", false);

	            getView().setValue("importe", vuelto);
	            getView().setEditable("importe", false);
	            addActions("MovimientoCaja.ActualizarCajaSalida", "Dialog.cancel");

	        } catch (Exception ex) {
	            ex.printStackTrace();
	            addError("system_error");
	        }
	    }

	    /**
	     * Método para obtener el ID de un movimiento por su nombre, creando uno si no existe.
	     *
	     * @param nombre El nombre del movimiento a buscar o crear.
	     * @return El ID del movimiento.
	     * @throws Exception Si ocurre un error durante el proceso.
	     */
	    private String obtenerOCrearMovimiento(String nombre) throws Exception {
	        EntityManager em = XPersistence.getManager();
	        String movimientoId;

	        try {
	            // Intentar obtener el ID del movimiento por su nombre
	            movimientoId = em.createQuery(
	                    "SELECT c.id FROM MovimientoCaja c WHERE c.nombre = :nombre", String.class)
	                    .setParameter("nombre", nombre)
	                    .getSingleResult();
	        } catch (NoResultException e) {
	            // Si no existe, crear el movimiento
	            MovimientoCaja nuevoMovimiento = new MovimientoCaja();
	            nuevoMovimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
	            nuevoMovimiento.setNombre(nombre);
	            nuevoMovimiento.setDescripcion("Salida de Efectivo por devolucion de un Vuelto en el Ingreso de Caja" );
	            // Setear otros valores si es necesario
	            em.persist(nuevoMovimiento);
	            em.flush(); // Forzar la generación del ID
	            movimientoId = nuevoMovimiento.getId();
	        }

	        return movimientoId;
	    }
	}