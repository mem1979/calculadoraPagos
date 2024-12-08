package com.sta.cashtill.enums;

public enum EstrategiaPagos {
	MEJOR_AJUSTE("Mejor distribucion de billetes calculada"),
	MAYOR_CANTIDAD("Denominaciones con Mayor Cantidad de Billetes"),
	MAYOR_VALOR("Denominaciones de Mayor Valor"),
	MENOR_VALOR("Denominaciones de Menor Valor"),
	MANUAL("Asignacion manual de billetes y cantidades");
	
	

    private String estrategiaPagos;

    // Constructor
    EstrategiaPagos(String estrategiaPagos) {
        this.estrategiaPagos = estrategiaPagos;
    }

    // M�todo para obtener la descripci�n
    public String getEstrategiaPagos() {
        return estrategiaPagos;
    }

    // Sobrescribimos el m�todo toString para devolver la descripci�n
    @Override
    public String toString() {
        return estrategiaPagos;
    }
}


