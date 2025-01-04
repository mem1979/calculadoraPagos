package com.sta.cashstill.acciones;

import org.openxava.actions.*;

import com.sta.cashstill.enums.*;



public class CategoriaEditarAction extends SearchByViewKeyAction {
	
	@Override
    public void execute() throws Exception {
       
	  super.execute(); // Llama a la lógica base para la creación de una nueva entidad

       
	// Obtener el nombre del modelo desde donde se ejecuta la acción
	    String modelName = getPreviousView().getModelName();
	    getView().setTitleId("Editar Categoria"); 
    // Determinar el tipo de movimiento según el modelo
        if ("CajaEntrada".equals(modelName)) {
        	getView().setValue("tipoMovimiento", TipoMovimiento.ENTRADA);
        } else if ("CajaSalida".equals(modelName)) {
        	getView().setValue("tipoMovimiento", TipoMovimiento.SALIDA);
        } else {
            // Opcional: manejo de casos donde el modelo no sea reconocido
            throw new IllegalStateException("Modelo no reconocido: " + modelName);
        }
    }
}


