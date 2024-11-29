package com.sta.cashtill.calculadores;

import java.math.*;

import org.openxava.calculators.*;
import org.openxava.jpa.*;

public class TotalCajaCalculador implements ICalculator {

    private static final long serialVersionUID = 1L;

    @Override
    public Object calculate() throws Exception {
	
    BigDecimal totalGeneral = (BigDecimal) XPersistence.getManager()
            .createQuery("SELECT SUM(c.total) FROM Caja c")
            .getSingleResult();

        return totalGeneral != null ? totalGeneral : BigDecimal.ZERO;

    }
}
