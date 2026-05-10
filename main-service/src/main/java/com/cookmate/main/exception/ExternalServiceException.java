package com.cookmate.main.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String serviceName, Throwable cause) {
        super(
            HttpStatus.BAD_GATEWAY,
            ErrorCode.EXTERNAL_SERVICE_ERROR,
            "Error calling external service: " + serviceName,
            cause
        );
    }
}
