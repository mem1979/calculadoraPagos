package com.sta.cashtill.enums;

public enum EstrategiaPagos {
	MAYOR_CANTIDAD("Denominaciones con Mayor Cantidad de Billetes"),
	MAYOR_VALOR("Denominaciones de Mayor Valor"),
	MENOR_VALOR("Denominaciones de Menor Valor"),
	EQUITATIVO("Distribucion Equitativa de Billetes");
	

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


