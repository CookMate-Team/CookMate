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

    @ExceptionHandler(SimulationSessionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleSimulationSessionNotFoundException(
            SimulationSessionNotFoundException exception
    ) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "SIMULATION_NOT_FOUND",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidSimulationStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidSimulationStateException(
            InvalidSimulationStateException exception
    ) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "INVALID_SIMULATION_STATE",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
}
