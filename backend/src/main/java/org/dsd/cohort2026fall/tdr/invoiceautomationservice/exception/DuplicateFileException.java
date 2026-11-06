package org.dsd.cohort2026fall.tdr.invoiceautomationservice.exception;

public class DuplicateFileException extends RuntimeException {
    public DuplicateFileException(String message) {
        super(message);
    }
}