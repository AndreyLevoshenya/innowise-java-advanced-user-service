package com.inno.service.exception;

public enum ExceptionErrorCodes {
    USER_DOES_NOT_EXIST("000001", "User with id %d does not exist"),
    PAYMENT_CARD_DOES_NOT_EXIST("000002", "Payment card with id %d does not exist"),
    TOO_MANY_PAYMENT_CARDS_EXCEPTION("000003", "Too many payment cards for user. Limit is 5"),
    VALIDATION_EXCEPTION("000004", "Validation failed: %s"),
    RESOURCE_NOT_FOUND("000005", "Resource not found"),
    METHOD_ARGUMENT_TYPE_MISMATCH_EXCEPTION("000006", "Id url path should be a number: %s"),
    DATA_INTEGRITY_EXCEPTION("000007", "Operation violates a data integrity constraint");

    private final String errorCode;
    private final String errorMessage;

    ExceptionErrorCodes(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }
}