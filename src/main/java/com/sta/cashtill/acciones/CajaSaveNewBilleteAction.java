package com.sta.cashtill.acciones;

import org.openxava.actions.*;

public class CajaSaveNewBilleteAction extends SaveAction{

	@Override
    public void execute() throws Exception {
		super.execute();
		closeDialog();
		
	}

}
