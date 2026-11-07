package org.dsd.cohort2026fall.tdr.invoiceautomationservice.controller;

import org.dsd.cohort2026fall.tdr.invoiceautomationservice.service.InvoiceAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

// This controller is just for testing, it should be deleted once this logic is integrated into the Invoice flow
@RestController("/ai")
public class AiTestController {

    @Autowired
    private InvoiceAnalysisService invoiceAnalysisService;

    @GetMapping("/analyze/{invoiceId}")
    public ResponseEntity<String> analyzeInvoice(@RequestParam("invoiceId") UUID invoiceId) {
     return ResponseEntity.ok(invoiceAnalysisService.analyzeInvoice(invoiceId));
    }



}
