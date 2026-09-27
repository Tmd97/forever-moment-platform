package com.forvmom.common.errorhandler;

public class IdempotencyRequestInProgressException extends RuntimeException {

    public IdempotencyRequestInProgressException(String message) {
        super(message);
    }
}
