package com.luigimonteforte.conservationrequests.exception;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException(String message) {
        super(message);
    }

    public DuplicateRequestException(String duplicateRequest, Throwable e) {
        super(duplicateRequest, e);
    }
}