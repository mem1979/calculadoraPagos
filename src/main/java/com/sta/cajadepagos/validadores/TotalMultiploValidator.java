package com.sta.cajadepagos.validadores;

import java.math.*;

import org.openxava.jpa.*;
import org.openxava.util.*;
import org.openxava.validators.*;

import com.sta.cajadepagos.modelo.*;

public class TotalMultiploValidator implements IPropertyValidator {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void validate(Messages errors, Object value, String modelName, String propertyName) throws Exception {
        // Si el valor es nulo, no se realiza la validación
        if (value == null) return;

        // El valor de la propiedad debe ser un BigDecimal
        BigDecimal total = (BigDecimal) value;

        // Si el modelo no es el esperado, no se realiza la validación
        if (!"Caja".equals(modelName)) return;

        // Recuperar la entidad actual desde XPersistence o similar
        Caja caja = (Caja) XPersistence.getManager().getReference(Class.forName("com.sta.cajadepagos.modelo.Caja"), modelName);

        // Validar que la denominación esté configurada
        if (caja.getDenominacion() == null || caja.getDenominacion().getValor() == null) {
            errors.add("denominacion_no_valida", propertyName, modelName);
            return;
        }

        BigDecimal valor = caja.getDenominacion().getValor();

        // Validar que el valor de la denominación sea mayor que cero
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("denominacion_cero");
            return;
        }

        // Validar que el total sea un múltiplo de la denominación
        BigDecimal remainder = total.remainder(valor);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            errors.add(
                "total_no_valido",
                propertyName,
                valor,
                valor.multiply(BigDecimal.valueOf(2)),
                valor.multiply(BigDecimal.valueOf(3))
            );
        }
    }
}
