package com.sta.cashtill.enums;

public enum EstrategiaPagos {
	MEJOR_AJUSTE("Mejor distribucion de billetes calculada"),
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


