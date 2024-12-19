package com.sta.cashtill.enums;

import org.openxava.model.*;

public enum TipoMovimiento implements IIconEnum {
    ENTRADA("currency-usd"),
    SALIDA("currency-usd-off"),
	AJUSTE("sync-alert");
   

    private String icon;

    TipoMovimiento(String icon) {
        this.icon = icon;
    }

    @Override
    public String getIcon() {
        return icon;
    }
}