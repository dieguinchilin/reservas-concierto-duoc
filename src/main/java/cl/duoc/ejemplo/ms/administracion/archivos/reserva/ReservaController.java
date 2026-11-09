package cl.duoc.ejemplo.ms.administracion.archivos.reserva;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final TicketPdfService ticketPdfService;
    private final EfsService efsService;
    private final AwsS3Service awsS3Service;

    @Value("${reservas.bucket:}")
    private String bucket;

    @Value("${reservas.subir-s3:false}")
    private boolean subirS3;

    public ReservaController(
            TicketPdfService ticketPdfService,
            EfsService efsService,
            AwsS3Service awsS3Service) {

        this.ticketPdfService = ticketPdfService;
        this.efsService = efsService;
        this.awsS3Service = awsS3Service;
    }

    @PostMapping("/ticket")
    public ResponseEntity<byte[]> generarTicket(
            @RequestBody ReservaSolicitud solicitud)
            throws IOException {

        // Validar los datos
        if (solicitud == null
                || solicitud.nombreCliente() == null
                || solicitud.nombreCliente().isBlank()
                || solicitud.correoCliente() == null
                || solicitud.correoCliente().isBlank()
                || solicitud.nombreEvento() == null
                || solicitud.nombreEvento().isBlank()
                || solicitud.cantidadEntradas() <= 0) {

            return ResponseEntity.badRequest().build();
        }

        // Crear reserva
        Reserva reserva = new Reserva(
                solicitud.nombreCliente(),
                solicitud.correoCliente(),
                solicitud.nombreEvento(),
                solicitud.cantidadEntradas()
        );

        // Generar PDF
        byte[] pdf = ticketPdfService.generarTicket(reserva);

        // Nombre unico para cada ticket
        String nombreArchivo =
                "ticket-" + reserva.getId() + ".pdf";

        // Guardar archivo en carpeta configurada
        efsService.guardarTicketPdf(nombreArchivo, pdf);

        // Subir PDF a AWS S3 si esta habilitado
        if (subirS3) {

            if (bucket == null || bucket.isBlank()) {
                throw new IllegalStateException(
                        "No se ha configurado reservas.bucket");
            }

            String claveS3 = "tickets/" + nombreArchivo;

            awsS3Service.subirTicketPdf(
                    bucket,
                    claveS3,
                    pdf
            );
        }

        // Devolver el PDF al usuario
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    public record ReservaSolicitud(
            String nombreCliente,
            String correoCliente,
            String nombreEvento,
            int cantidadEntradas
    ) {
    }
}