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

    // Método para obtener la descripción
    public String getEstrategiaPagos() {
        return estrategiaPagos;
    }

    // Sobrescribimos el método toString para devolver la descripción
    @Override
    public String toString() {
        return estrategiaPagos;
    }
}


