package com.sta.cajadepagos.auditoria;

import java.math.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.*;
import org.openxava.annotations.*;

import lombok.*;

@View(members = "fechaHora , usuario;" +
				"movimintoTipo,  operacion;" +
				"total, totalCaja ")

@Tab(properties = "fechaHora , usuario, movimintoTipo,  operacion, totalCaja, total+",
editors = "List",
defaultOrder = "${fechaHora} asc")

@Entity
@Table(name = "CAJA_AUD")
@Getter @Setter
public class CajaAuditoria {
		
    @Id
    @Column(name = "ID")
    private String id;
       
    @Column( name = "USUARIO")
    String usuario;

    @Column( name = "MOVIMINTOTIPO")
    String movimintoTipo;
    
    @Column(name = "FECHAHORA")
    @DateTime
    private Date fechaHora;

    @Column(name = "CANTIDAD")
    private Integer cantidad;

    @Column(name = "TOTAL")
    private BigDecimal total;
    
    @Column(name = "TOTALGENERAL")
    private BigDecimal totalCaja;

    @Formula("(CASE WHEN revtype = 0 THEN 'NUEVO' WHEN revtype = 1 THEN 'MODIFICADO' WHEN revtype = 2 THEN 'BORRADO' ELSE 'DESCONOCIDO' END)")
    private String operacion;

   }
