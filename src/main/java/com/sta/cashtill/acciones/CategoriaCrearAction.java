package com.sta.cashtill.acciones;

import org.openxava.actions.*;

// Importar el enum
import com.sta.cashtill.enums.*;

public class CategoriaCrearAction extends NewAction {

	
    
	@Override
	    public void execute() throws Exception {
	       
		  super.execute(); // Llama a la l�gica base para la creaci�n de una nueva entidad

	       
		// Obtener el nombre del modelo desde donde se ejecuta la acci�n
		    String modelName = getPreviousView().getModelName();
		    getView().setTitleId("Ingresar nueva Categoria"); 
	    // Determinar el tipo de movimiento seg�n el modelo
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