package com.sta.cajadepagos.calculadores;

import org.openxava.calculators.*;
import org.openxava.jpa.*;

import com.sta.cajadepagos.modelo.*;

public class DenominacionPorDefectoCalculator implements ICalculator {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public Object calculate() throws Exception {
        // Consulta para obtener la primera denominación ordenada por "valor"
        return XPersistence.getManager()
            .createQuery("SELECT d FROM Denominaciones d ORDER BY d.valor ASC", Denominaciones.class)
            .setMaxResults(1)
            .getSingleResult();
    }
}