package org.dsd.cohort2026fall.tdr.invoiceautomationservice.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.config.MinioProperties;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception.StorageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private static final String KEY_PREFIX = "invoices/";

    private final MinioClient minioClient;
    private final MinioProperties props;

    private volatile boolean bucketReady = false;

    /** Uploads the file and returns its object key, e.g. "invoices/3f2a...pdf". */
    public String store(MultipartFile file, String extension) {
        ensureBucket();
        String key = KEY_PREFIX + UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to store file in MinIO", e);
        }
    }

    /** Opens the stored file. Caller must close the stream. */
    public InputStream load(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to load file: " + key, e);
        }
    }

    /** Used to clean up if the database save fails after upload. */
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + key, e);
        }
    }

    // Checked on first use instead of at startup, so a missing
    // MinIO never stops teammates' apps from booting.
    private void ensureBucket() {
        if (bucketReady) return;
        synchronized (this) {
            if (bucketReady) return;
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(props.bucket()).build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(props.bucket()).build());
                }
                bucketReady = true;
            } catch (Exception e) {
                throw new StorageException("MinIO unavailable or bucket setup failed", e);
            }
        }
    }
}