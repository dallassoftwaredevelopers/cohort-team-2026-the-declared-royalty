package org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
