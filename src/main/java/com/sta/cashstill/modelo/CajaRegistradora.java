package com.sta.cashstill.modelo;

import java.math.*;
import java.text.*;
import java.util.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.calculators.*;
import org.openxava.model.*;

import com.sta.cashstill.auxiliares.*;
import com.sta.cashstill.calculadores.*;
import com.sta.cashstill.enums.*;

import lombok.*;




@Tab(properties = "usuario, fechaHora, tipoMovimiento, categoria, total+",
	 editors = "List, Calendar",
	 defaultOrder = "${fechaHora} desc",
	 rowStyles= { @RowStyle(style="row-entrada", property="tipoMovimiento", value="ENTRADA"),
				  @RowStyle(style="row-SALIDA",  property="tipoMovimiento", value="SALIDA"),
				  @RowStyle(style="row-ajuste",  property="categoria", value="VUELTO")
				 })

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
abstract public class CajaRegistradora extends Identifiable {
	
		
	@DateTime
    @ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(CurrentDateCalculator.class)
    private Date fechaHora;
	
	@ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @DefaultValueCalculator(UsuarioPorDefectoCalculator.class)
	String usuario;
	
	@TextArea
	@Column(length = 1000)
	String descripcion;
	
    @Money
    @Required
    @LabelFormat(notForViews = "salida", value = LabelFormatType.SMALL)
    @DefaultValueCalculator(value = ZeroBigDecimalCalculator.class)
    private BigDecimal importe;
    
   
    @ReadOnly
    @DefaultValueCalculator(FalseCalculator.class)
    @Column(columnDefinition="BOOLEAN DEFAULT FALSE") // Para llenar con falses en lugar de con nulos
    boolean conVuelto;
    
    @Money
    @ReadOnly
    @LabelFormat(LabelFormatType.SMALL)
    @Calculation("sum(detalle.total) - importe")
    private BigDecimal vuelto;
	
	@LabelFormat(LabelFormatType.SMALL)
	@File(maxFileSizeInKb=90)
	@Column(length=32)
	private String documento;
	
	
	@ElementCollection
    @CollectionTable(
            name = "CAJAREGISTRADORA_DETALLE",  // Tabla intermedia
            joinColumns = @JoinColumn(name = "CAJAREGISTRADORA_ID") 
        )
	@ListProperties(forViews="entrada", value = "caja, cantidad, total+[cajaRegistradora.vuelto]" )
	@ListProperties(forViews="salida", value = "caja, cantidad, total+" )
	private List <DetalleCajaRegistradora> detalle = new ArrayList<>();
	
	// Propiedad calculada para sumar el total de los detalles
	@Depends("movimiento")
	@Money
	@ReadOnly
	public BigDecimal getTotalDetalle() {
	    if (detalle == null || detalle.isEmpty()) {
	        return BigDecimal.ZERO;
	    }

	    // Sumar los valores de 'total' en la colección 'detalle'
	    BigDecimal total = detalle.stream()
	            .map(d -> d.getTotal() != null ? d.getTotal() : BigDecimal.ZERO)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

	    // Aplicar el signo según el movimiento
	    String movimiento = getMovimiento();
	    if ("SALIDA".equalsIgnoreCase(movimiento)) {
	        return total.negate(); // Hacer el total negativo
	    }

	    return total; // Mantener el total positivo si es "ENTRADA" u otro valor
	}
	
	@Money
	@LabelFormat(LabelFormatType.NO_LABEL)
	@LargeDisplay(icon = "cash-multiple")
	BigDecimal total;
	
	@Enumerated(EnumType.STRING)
	private TipoMovimiento tipoMovimiento;
	
	@LabelFormat(LabelFormatType.NO_LABEL)
	@LargeDisplay(icon = "cash-register")
	@Column(length = 30)
	String categoria;
	
	@PreUpdate @PrePersist
    private void Actualizar() {
	   	setUsuario(getObtenerNombreUsuario());
	   	setFechaHora(new Date());	   
	   	setTotal(getTotalDetalle());
	   	setCategoria(getMovimientoCajaCategoria());
	   	setDescripcion(getGenerarDescripcion(getObtenerNombreUsuario(), fechaHora));
	    String movimientoCalculado = getMovimiento(); // Ejemplo: "entrada", "salida", etc.
	    switch (movimientoCalculado) {
	        case "ENTRADA":
	            setTipoMovimiento(TipoMovimiento.ENTRADA);
	            break;
	        case "SALIDA":
	            setTipoMovimiento(TipoMovimiento.SALIDA);
	            break;
	        case "AJUSTE":
	            setTipoMovimiento(TipoMovimiento.AJUSTE);
	            break;
	        default:
	            throw new IllegalStateException("Movimiento desconocido: " + movimientoCalculado);
	    }
	}
	
 // Métodos auxiliares
	
 // Obtiene el Nombre del Usuario
	@Transient
    private String getObtenerNombreUsuario() {
    return org.openxava.util.Users.getCurrent() != null ? org.openxava.util.Users.getCurrent() : "No Registrado";
    }
    
    @Transient
    @ReadOnly
 // Obtiene el tipo de Movimiento segun Dtype
    public String getMovimiento(){
        // Retorna el valor de discriminador directamente
        return this.getClass().getAnnotation(DiscriminatorValue.class) != null
               ? this.getClass().getAnnotation(DiscriminatorValue.class).value() : null;
        }
   
   @Transient
   @ReadOnly
// Obtiene la Categoria de movimientoCaja
   public String getMovimientoCajaCategoria() {
	    if (this instanceof CajaEntrada) {
	        CajaEntrada cajaEntrada = (CajaEntrada) this;
	        return cajaEntrada.getMovimientoCaja() != null ? cajaEntrada.getMovimientoCaja().getNombre() : null;
	    } else if (this instanceof CajaSalida) {
	        CajaSalida cajaSalida = (CajaSalida) this;
	        return cajaSalida.getMovimientoCaja() != null ? cajaSalida.getMovimientoCaja().getNombre() : null;
	    }
	    return null;
	}
   
      
   public String getGenerarDescripcion(String usuario, Date fecha) {
	   // Obtener la descripción actual si existe
       String descripcionActual = Optional.ofNullable(getDescripcion()).orElse("");
	   
	    // Obtener fecha formateada
	    String fechaFormateada = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(fecha);

	    // Construir encabezado
	    StringBuilder detalleDescripcion = new StringBuilder(
	        String.format("%s.\nRegistrado por %s el %s\n", descripcionActual, usuario, fechaFormateada)
	    );

	    // Definir el orden esperado de los IDs de caja
	    List<String> ordenDenominaciones = Arrays.asList(
	        "$50Mil.-", "$20Mil.-", "$10Mil.-", "$5Mil.-", "$2Mil.-", "$1Mil.-",
	        "$500.-", "$200.-", "$100.-", "$50.-", "$20.-", "$10.-", "$5.-"
	    );

	    // Unificar los detalles con el mismo ID de caja
	    Map<String, DetalleCajaRegistradora> detalleUnificado = new HashMap<>();
	    for (DetalleCajaRegistradora detalle : this.detalle) {
	        String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getDenominacion).orElse("N/A");
	        DetalleCajaRegistradora existente = detalleUnificado.get(idCaja);

	        if (existente == null) {
	            // Crear un nuevo detalle unificado
	            DetalleCajaRegistradora nuevoDetalle = new DetalleCajaRegistradora();
	            nuevoDetalle.setCaja(detalle.getCaja());
	            nuevoDetalle.setCantidad(detalle.getCantidad());
	            nuevoDetalle.setTotal(detalle.getTotal());
	            detalleUnificado.put(idCaja, nuevoDetalle);
	        } else {
	            // Sumar cantidades y totales al detalle existente
	            existente.setCantidad(existente.getCantidad() + detalle.getCantidad());
	            existente.setTotal(existente.getTotal().add(detalle.getTotal()));
	        }
	    }

	    // Ordenar los detalles unificados según el orden de denominaciones
	    List<DetalleCajaRegistradora> detallesOrdenados = new ArrayList<>(detalleUnificado.values());
	    detallesOrdenados.sort((d1, d2) -> {
	        String idCaja1 = Optional.ofNullable(d1.getCaja()).map(Caja::getDenominacion).orElse("N/A");
	        String idCaja2 = Optional.ofNullable(d2.getCaja()).map(Caja::getDenominacion).orElse("N/A");
	        int index1 = ordenDenominaciones.indexOf(idCaja1);
	        int index2 = ordenDenominaciones.indexOf(idCaja2);
	        return Integer.compare(index2, index1); // Ordenar de mayor a menor
	    });

	    // Construir el detalle de la descripción en formato tabular
	    detalleDescripcion.append(String.format("%-20s%-15s%-15s\n", "Billete", "Cantidad", "Total"));
	    detalleDescripcion.append("---------------------------------------------------\n");
	    for (DetalleCajaRegistradora detalle : detallesOrdenados) {
	        String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getDenominacion).orElse("N/A");
	        int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
	        BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

	        detalleDescripcion.append(String.format(
	            "%-20s%-15d$%-14s\n",
	            idCaja, cantidad, total.toPlainString()
	        ));
	    }

	    return detalleDescripcion.toString();
	}

}
