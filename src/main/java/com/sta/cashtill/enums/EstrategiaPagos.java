package com.sta.cashtill.enums;

public enum EstrategiaPagos {
	MEJOR_AJUSTE("Mejor distribucion de billetes calculada"),
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


