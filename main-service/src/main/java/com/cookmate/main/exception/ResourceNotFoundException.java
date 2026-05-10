package com.cookmate.main.exception;

import org.springframework.http.HttpStatus;

public abstract class ResourceNotFoundException extends ApiException {

    protected ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
}
