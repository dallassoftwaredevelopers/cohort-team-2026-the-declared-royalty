package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.AiRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

// This controller is just for testing, I'll delete it later
@RestController("/temp")
public class TempController {

    @Autowired
    private AiRequestService aiRequestService;

    @GetMapping("/analyze/{invoiceId}")
    public ResponseEntity<String> analyzeInvoice(@RequestParam("invoiceId") UUID invoiceId) {
     return ResponseEntity.ok(aiRequestService.analyzeInvoice(invoiceId));
    }



}
