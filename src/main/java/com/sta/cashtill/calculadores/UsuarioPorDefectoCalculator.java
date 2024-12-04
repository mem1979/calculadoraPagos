package com.sta.cashtill.calculadores;

import org.openxava.calculators.*;

public class UsuarioPorDefectoCalculator implements ICalculator {

    private static final long serialVersionUID = 1L;

    @Override
    public Object calculate() throws Exception {
		 return org.openxava.util.Users.getCurrent() != null ? org.openxava.util.Users.getCurrent() : "No Registrado";
    }
}
