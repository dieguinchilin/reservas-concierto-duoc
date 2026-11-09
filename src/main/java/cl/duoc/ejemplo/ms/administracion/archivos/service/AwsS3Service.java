package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidFileException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3AccessDeniedException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3BucketNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3ObjectNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3OperationException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3UploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;

    // LISTAR ARCHIVOS DE S3
    public List<S3ObjectDto> listObjects(String bucket) {

        try {
            log.info("Listando objetos del bucket: {}", bucket);

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            log.info("Se encontraron {} objetos en el bucket {}",
                    response.contents().size(), bucket);

            return response.contents().stream()
                    .map(obj -> new S3ObjectDto(
                            obj.key(),
                            obj.size(),
                            obj.lastModified() != null
                                    ? obj.lastModified().toString()
                                    : null))
                    .collect(Collectors.toList());

        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "listar objetos del bucket: " + bucket, e);
            }

            throw new S3OperationException(
                    "Error al listar objetos del bucket: " + bucket, e);
        }
    }

    // DESCARGAR ARCHIVOS DESDE S3
    public byte[] downloadAsBytes(String bucket, String key) {

        try {
            log.info("Descargando objeto: {} del bucket: {}", key, bucket);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObjectAsBytes(request);

            log.info("Objeto descargado exitosamente: {}", key);

            return response.asByteArray();

        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);

        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(key, bucket, e);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "descargar el objeto: " + key, e);
            }

            throw new S3OperationException(
                    "Error al descargar el objeto: " + key, e);
        }
    }

    // SUBIR ARCHIVOS A S3 (METODO ORIGINAL)
    public void upload(String bucket, String key, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException(
                    "El archivo está vacío o es nulo");
        }

        if (file.getOriginalFilename() == null
                || file.getOriginalFilename().isBlank()) {
            throw new InvalidFileException(
                    "El nombre del archivo no es válido");
        }

        if (file.getSize() == 0) {
            throw new InvalidFileException(
                    "El archivo no puede tener tamaño 0");
        }

        try {
            log.info("Subiendo archivo: {} al bucket: {}, tamaño: {} bytes",
                    key, bucket, file.getSize());

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()));

            log.info("Archivo subido exitosamente: {}", key);

        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "subir archivo al bucket: " + bucket, e);
            }

            throw new S3UploadException(
                    "Error al subir el archivo a S3: " + e.getMessage(), e);

        } catch (IOException e) {
            throw new S3UploadException(
                    "Error al leer el archivo: " + e.getMessage(), e);
        }
    }

    // NUEVO METODO: SUBIR TICKETS PDF A S3
    public void subirTicketPdf(
            String bucket,
            String key,
            byte[] contenidoPdf) {

        if (contenidoPdf == null || contenidoPdf.length == 0) {
            throw new InvalidFileException(
                    "El ticket PDF está vacío");
        }

        try {
            log.info("Subiendo ticket PDF {} al bucket {}",
                    key, bucket);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .contentLength((long) contenidoPdf.length)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(contenidoPdf));

            log.info("Ticket PDF subido correctamente a S3: {}", key);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "subir ticket PDF al bucket: " + bucket, e);
            }

            throw new S3UploadException(
                    "Error al subir el ticket PDF a S3: "
                            + e.getMessage(), e);
        }
    }

    // MOVER ARCHIVOS DENTRO DEL MISMO BUCKET
    public void moveObject(
            String bucket,
            String sourceKey,
            String destKey) {

        try {
            log.info("Moviendo objeto de {} a {} en el bucket: {}",
                    sourceKey, destKey, bucket);

            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(destKey)
                    .build();

            s3Client.copyObject(request);

            log.info("Objeto copiado, eliminando el origen");

            deleteObject(bucket, sourceKey);

            log.info("Objeto movido exitosamente");

        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);

        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(
                    sourceKey, bucket, e);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "mover objeto en el bucket: " + bucket, e);
            }

            throw new S3OperationException(
                    "Error al mover el objeto de "
                            + sourceKey + " a " + destKey, e);
        }
    }

    // ELIMINAR ARCHIVOS DE S3
    public void deleteObject(String bucket, String key) {

        try {
            log.info("Eliminando objeto: {} del bucket: {}",
                    key, bucket);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            log.info("Objeto eliminado exitosamente: {}", key);

        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);

        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw new S3AccessDeniedException(
                        "eliminar objeto del bucket: " + bucket, e);
            }

            throw new S3OperationException(
                    "Error al eliminar el objeto: " + key, e);
        }
    }
}