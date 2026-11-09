package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EfsService {

    @Value("${efs.path}")
    private String efsPath;

    // Metodo original del profesor
    public File saveToEfs(String filename, MultipartFile multipartFile)
            throws IOException {

        File dest = new File(efsPath, filename);
        File parentDir = dest.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            Files.createDirectories(parentDir.toPath());
        }

        multipartFile.transferTo(dest);
        return dest;
    }

    // Nuevo metodo para guardar tickets PDF
    public File guardarTicketPdf(String nombreArchivo, byte[] contenidoPdf)
            throws IOException {

        Path directorio = Paths.get(efsPath).toAbsolutePath().normalize();
        Files.createDirectories(directorio);

        Path destino = directorio.resolve(nombreArchivo).normalize();

        if (!destino.getParent().equals(directorio)) {
            throw new IOException("Nombre de archivo no permitido");
        }

        Files.write(destino, contenidoPdf);

        return destino.toFile();
    }
}