package com.sta.cajadepagos.calculadores;

import java.time.*;

import org.openxava.calculators.*;

public class CurrentLocalDateTimeCalculator implements ICalculator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Object calculate() throws Exception {
		return LocalDateTime.now();
	}

}
