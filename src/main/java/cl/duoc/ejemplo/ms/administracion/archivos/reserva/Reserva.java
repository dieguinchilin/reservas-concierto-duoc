
package cl.duoc.ejemplo.ms.administracion.archivos.reserva;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reserva {

    private String id;
    private String nombreCliente;
    private String correoCliente;
    private String nombreEvento;
    private int cantidadEntradas;
    private LocalDateTime fechaReserva;

    public Reserva() {
    }

    public Reserva(String nombreCliente, String correoCliente,
                   String nombreEvento, int cantidadEntradas) {

        this.id = UUID.randomUUID().toString();
        this.nombreCliente = nombreCliente;
        this.correoCliente = correoCliente;
        this.nombreEvento = nombreEvento;
        this.cantidadEntradas = cantidadEntradas;
        this.fechaReserva = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public int getCantidadEntradas() {
        return cantidadEntradas;
    }

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }
}