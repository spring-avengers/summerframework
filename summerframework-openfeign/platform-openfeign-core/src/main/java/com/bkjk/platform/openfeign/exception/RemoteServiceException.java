package com.bkjk.platform.openfeign.exception;

import feign.codec.DecodeException;

public class RemoteServiceException extends DecodeException {
    private String errorCode;
    private String errorMessage;

    public RemoteServiceException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public RemoteServiceException(Throwable cause, String errorCode, String errorMessage) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
