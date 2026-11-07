package org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception;

public class InvoiceAlreadyExistsException extends RuntimeException {

    public InvoiceAlreadyExistsException(String message) {
        super(message);
    }
}
