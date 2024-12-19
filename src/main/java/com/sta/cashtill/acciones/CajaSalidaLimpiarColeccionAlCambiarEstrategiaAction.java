package com.sta.cashtill.acciones;

import java.util.*;

import org.openxava.actions.*;

import com.sta.cashtill.enums.*;
import com.sta.cashtill.modelo.*;

public class CajaSalidaLimpiarColeccionAlCambiarEstrategiaAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        // Obtén el nuevo valor como EstrategiaPagos en lugar de String
        EstrategiaPagos estrategiaPagos = (EstrategiaPagos) getNewValue();

        // Verifica si la propiedad tiene un valor
        if (estrategiaPagos != null) {
            // Limpia la colección detalle
            @SuppressWarnings("unchecked")
            List<DetalleCajaRegistradora> detalle = (List<DetalleCajaRegistradora>) getView().getValue("detalle");

            if (detalle != null) {
                detalle.clear();
                // Actualiza la vista para reflejar los cambios
                getView().refreshCollections();
            }
        }
    }
}

