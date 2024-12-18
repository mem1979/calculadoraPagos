package com.sta.cashtill.acciones;

import java.math.*;
import java.text.*;
import java.util.*;

import org.openxava.actions.*;
import org.openxava.jpa.*;
import org.openxava.util.*;

import com.sta.cashtill.modelo.*;

public class ActualizarCajaDesdeCajaEntradaAction extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Obtener la entidad CajaEntrada desde la vista
        CajaEntrada cajaEntrada = (CajaEntrada) getView().getEntity();

        if (cajaEntrada == null) {
            addError("No se encontró la entidad CajaEntrada. Asegúrate de que está correctamente configurada.");
            System.err.println("Error: CajaEntrada es nula. Verificar configuración en la vista.");
            return;
        }

        // Validar que haya detalles para procesar
        if (cajaEntrada.getDetalle() == null || cajaEntrada.getDetalle().isEmpty()) {
            addError("No se especificaron billetes para ingresar a Caja.");
            return;
        }

        // Generar descripción y setearla en la vista antes de guardar
        String descripcionGenerada = generarDescripcion(cajaEntrada);
        getView().setValue("descripcion", descripcionGenerada);

        // Intentar guardar la entidad principal
        try {
            System.out.println("Guardando entidad principal CajaEntrada...");
            super.execute();
            System.out.println("Entidad principal guardada exitosamente.");
        } catch (Exception e) {
            addError("Error durante la ejecución de la acción base: " + e.getMessage());
            e.printStackTrace();
            return; // Detener si hay un error en el guardado
        }

        // Procesar cada detalle de la colección
        for (DetalleCajaRegistradora detalle : cajaEntrada.getDetalle()) {
            if (detalle == null) {
                addError("Uno de los detalles está vacío. Revisa el Detalle.");
                continue;
            }

            Caja caja = detalle.getCaja();
            if (caja == null) {
                addError("La relación Caja en el detalle es nula.");
                continue;
            }

            String cajaId = caja.getId();
            if (cajaId == null || cajaId.isEmpty()) {
                addError("El ID de la caja en el detalle es nulo o vacío.");
                continue;
            }

            // Buscar la caja persistida por ID
            Caja cajaPersistida = buscarCajaPorId(cajaId);
            if (cajaPersistida == null) {
                addError("No se encontró una caja con el ID: " + cajaId);
                continue;
            }

            // Validar valores actuales de la caja antes de operar
            if (cajaPersistida.getTotal() == null) {
                cajaPersistida.setTotal(BigDecimal.ZERO); // Inicializar total a 0 si es null
            }

            // Actualizar los valores de la caja con los datos del detalle
            int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
            BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

            cajaPersistida.setCantidad(cajaPersistida.getCantidad() + cantidad);
            cajaPersistida.setTotal(cajaPersistida.getTotal().add(total));

            // Persistir los cambios en la caja
            try {
                System.out.println("Actualizando caja con ID: " + cajaId);
                XPersistence.getManager().merge(cajaPersistida);
                System.out.println("Caja actualizada exitosamente.");
            } catch (Exception e) {
                addError("Error al actualizar la caja con ID " + cajaId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Cerrar el diálogo y mostrar un mensaje si no hay errores
        if (getErrors().isEmpty()) {
            closeDialog();
            addMessage("Billetes en caja, ingresados correctamente.");
        } else {
            addError("Ocurrieron errores durante la actualización de las cajas.");
        }
    }

    /**
     * Busca una caja en la base de datos utilizando su ID.
     *
     * @param cajaId ID de la caja a buscar.
     * @return Instancia de Caja encontrada o null si no existe.
     */
    private Caja buscarCajaPorId(String cajaId) {
        try {
            System.out.println("Buscando caja con ID: " + cajaId);
            Caja caja = XPersistence.getManager()
                    .createQuery("SELECT c FROM Caja c WHERE c.id = :cajaId", Caja.class)
                    .setParameter("cajaId", cajaId)
                    .getSingleResult();
            System.out.println("Caja encontrada: " + caja.getId());
            return caja;
        } catch (Exception e) {
            System.err.println("Error al buscar la caja con ID: " + cajaId + ". Excepción: " + e.getMessage());
            return null;
        }
    }

    /**
     * Genera una descripción automática basada en los detalles de la CajaEntrada.
     *
     * @param cajaEntrada Instancia de CajaEntrada.
     * @return La descripción generada.
     */
    private String generarDescripcion(CajaEntrada cajaEntrada) {
        // Obtener la descripción actual si existe
        String descripcionActual = Optional.ofNullable(cajaEntrada.getDescripcion()).orElse("");

        // Obtener nombre del usuario y fecha actual
        String usuario = Users.getCurrent();
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        StringBuilder detalleDescripcion = new StringBuilder(
                String.format("\nDetalle de billetes ingresados por %s el %s:\n", usuario, fecha));

        // Definir el orden esperado de los IDs de caja
        List<String> ordenDenominaciones = Arrays.asList(
        	"$50Mil.-", "$20Mil.-", "$10Mil.-", "$5Mil.-", "$2Mil.-", "$1Mil.-", "$500.-", "$200.-", "$100.-", "$50.-", "$20.-", "$10.-", "$5.-"
        );

        // Unificar los detalles con el mismo ID de caja en una estructura temporal
        Map<String, DetalleCajaRegistradora> detalleUnificado = new HashMap<>();
        for (DetalleCajaRegistradora detalle : cajaEntrada.getDetalle()) {
            String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getId).orElse("N/A");
            DetalleCajaRegistradora existente = detalleUnificado.get(idCaja);

            if (existente == null) {
                // Crear un nuevo detalle unificado en la estructura temporal
                DetalleCajaRegistradora nuevoDetalle = new DetalleCajaRegistradora();
                nuevoDetalle.setCaja(detalle.getCaja());
                nuevoDetalle.setCantidad(detalle.getCantidad());
                nuevoDetalle.setTotal(detalle.getTotal());
                detalleUnificado.put(idCaja, nuevoDetalle);
            } else {
                // Sumar cantidades y totales al detalle existente en la estructura temporal
                existente.setCantidad(existente.getCantidad() + detalle.getCantidad());
                existente.setTotal(existente.getTotal().add(detalle.getTotal()));
            }
        }

        // Ordenar los detalles unificados según el orden de denominaciones
        List<DetalleCajaRegistradora> detallesOrdenados = new ArrayList<>(detalleUnificado.values());
        detallesOrdenados.sort((d1, d2) -> {
            String idCaja1 = Optional.ofNullable(d1.getCaja()).map(Caja::getId).orElse("N/A");
            String idCaja2 = Optional.ofNullable(d2.getCaja()).map(Caja::getId).orElse("N/A");
            int index1 = ordenDenominaciones.indexOf(idCaja1);
            int index2 = ordenDenominaciones.indexOf(idCaja2);
            return Integer.compare(index2, index1); // Ordenar de mayor a menor
        });

        // Construir el detalle de la descripción en columnas
        detalleDescripcion.append(String.format("%-20s%-15s%-15s\n", "Billete", "Cantidad", "Total"));
        detalleDescripcion.append("---------------------------------------------------\n");
        for (DetalleCajaRegistradora detalle : detallesOrdenados) {
            String idCaja = Optional.ofNullable(detalle.getCaja()).map(Caja::getId).orElse("N/A");
            int cantidad = Optional.ofNullable(detalle.getCantidad()).orElse(0);
            BigDecimal total = Optional.ofNullable(detalle.getTotal()).orElse(BigDecimal.ZERO);

            detalleDescripcion.append(String.format(
                    "%-20s%-15d$%-14s\n",
                    idCaja, cantidad, total.toPlainString()
            ));
        }

        return descripcionActual + detalleDescripcion.toString();
    }
        

}

