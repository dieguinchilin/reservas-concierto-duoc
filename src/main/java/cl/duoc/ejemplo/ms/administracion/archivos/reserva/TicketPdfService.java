package cl.duoc.ejemplo.ms.administracion.archivos.reserva;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class TicketPdfService {

    public byte[] generarTicket(Reserva reserva) throws IOException {

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);

            try (PDPageContentStream contenido =
                    new PDPageContentStream(documento, pagina)) {

                PDType1Font titulo = new PDType1Font(
                        Standard14Fonts.FontName.HELVETICA_BOLD);

                PDType1Font texto = new PDType1Font(
                        Standard14Fonts.FontName.HELVETICA);

                contenido.beginText();
                contenido.setFont(titulo, 20);
                contenido.newLineAtOffset(50, 750);
                contenido.showText("ENTRADA - CONCIERTO");
                contenido.endText();

                escribirLinea(contenido, texto, 700,
                        "Codigo de reserva: " + reserva.getId());

                escribirLinea(contenido, texto, 670,
                        "Cliente: " + limpiar(reserva.getNombreCliente()));

                escribirLinea(contenido, texto, 640,
                        "Correo: " + limpiar(reserva.getCorreoCliente()));

                escribirLinea(contenido, texto, 610,
                        "Evento: " + limpiar(reserva.getNombreEvento()));

                escribirLinea(contenido, texto, 580,
                        "Cantidad de entradas: " + reserva.getCantidadEntradas());

                escribirLinea(contenido, texto, 550,
                        "Fecha de reserva: " +
                        reserva.getFechaReserva().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }

            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private void escribirLinea(PDPageContentStream contenido,
                               PDType1Font fuente,
                               float posicionY,
                               String mensaje) throws IOException {

        contenido.beginText();
        contenido.setFont(fuente, 12);
        contenido.newLineAtOffset(50, posicionY);
        contenido.showText(mensaje);
        contenido.endText();
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor.replaceAll("[^\\x20-\\x7E]", " ");
    }
}