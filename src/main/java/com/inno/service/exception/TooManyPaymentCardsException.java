package com.inno.service.exception;

public class TooManyPaymentCardsException extends RuntimeException {
    public TooManyPaymentCardsException(String message) {
        super(message);
    }
}
