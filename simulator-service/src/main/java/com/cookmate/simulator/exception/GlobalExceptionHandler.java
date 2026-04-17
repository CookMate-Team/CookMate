package com.cookmate.simulator.exception;

import com.cookmate.simulator.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MainServiceCommunicationException.class)
    public ResponseEntity<ErrorResponseDto> handleMainServiceCommunicationException(
            MainServiceCommunicationException exception
    ) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "MAIN_SERVICE_UNAVAILABLE",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
}
