package com.sta.cashstill.auxiliares;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import com.sta.cashstill.anotaciones.*;
import com.sta.cashstill.enums.*;

import lombok.*;

@View(name="Simple", members = "descripcion")

@Entity
@Getter @Setter
public class MovimientoCaja extends Identifiable{
		
	@Required
	@ReadOnly
	@LargeDisplay(icon="cash-multiple") 
	@LabelFormat(LabelFormatType.NO_LABEL)
	@Enumerated(EnumType.STRING)
	private TipoMovimiento tipoMovimiento;
	
	
	@NotNull(message = "El nombre de la Categoria es obligatoria")
	@Size(max = 30, message = "El Nombre de la CATEGORIA no puede exceder los 30 caracteres")
	@Column(length = 30)
	@LabelFormat(LabelFormatType.SMALL)
	@Mayuscula
	String nombre;
	
	@Size(max = 150, message = "La Descripción de la CATEGORIA no puede exceder los 150 caracteres")
	@Column(length = 150)
	@LabelFormat(value =  LabelFormatType.SMALL)
	String descripcion;
	

	
	
	}
