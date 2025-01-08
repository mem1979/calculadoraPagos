package com.sta.cashstill.acciones;



import com.sta.cashstill.modelo.*;

public class ActualizarCajaRegistrarMovimientoSalidaAction extends BaseActualizarCajaAction {

	
	
    @Override
    public void execute() throws Exception {
        
    	boolean esVuelto = "SALIDA DE EFECTIVO - VUELTO".equalsIgnoreCase(getView().getTitle().toString());

    	
   	// 1. Obtener la entidad actual desde la vista
          Object entidad = getView().getEntity();
          if (!(entidad instanceof CajaSalida)) {
              addError("No es una CajaSalida");
              return;
          }
          
          CajaSalida salida = (CajaSalida) entidad;

          // 3. Guardar la entidad (proceso normal)
          super.execute();

          // 4. Detener si hubo errores
          if (!getErrors().isEmpty()) {
              return;
          }

          // 5. Procesar el detalle
          procesarMovimiento(salida.getDetalle(), false);

          // 6. Cerrar el diálogo
          closeDialog();
          
          if(esVuelto) {
          getView().setModelName("CajaEntrada");
          getView().setViewName("entrada");
          getView().setValueNotifying("conVuelto", true);
          getView().setEditable(false);
          returnToPreviousControllers();
          addActions("MovimientoCaja.ActualizarCajaEntrada");
       //  executeAction("MovimientoCaja.ActualizarCajaEntrada");
          }
    }
}