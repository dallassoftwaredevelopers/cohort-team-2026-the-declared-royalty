package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import io.minio.GetObjectArgs;
import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.AiRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.minio.MinioClient;

import java.io.InputStream;
import java.util.UUID;

// This controller is just for testing, I'll delete it later
@RestController("/temp")
public class TempController {

    @Autowired
    private AiRequestService aiRequestService;

    @GetMapping("/analyze/{invoiceId}")
    public ResponseEntity<String> analyzeInvoice(@RequestParam("invoiceId") UUID invoiceId) {
     return ResponseEntity.ok(aiRequestService.analyzeInvoice(invoiceId));
//     0ef8a787-8931-4b88-8330-1c0908765da1.pdf
    }


    @GetMapping("/file")
    public ResponseEntity<byte[]> getFile(){

//        String filename = "burp.txt";
        String filename = "alder-creek-bill.pdf";

        byte[] file = getInvoice(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .body(file);
    }

    private byte[] getInvoice(String filename){

        try(
                InputStream stream = MinioClient.builder()
                        .endpoint("http://minio:9000")
                        .credentials("minio_user","minio_password")
                        .build()
                        .getObject(
                                GetObjectArgs.builder()
                                        .bucket("invoices")
                                        .object(filename)
                                        .build()
        )){
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
