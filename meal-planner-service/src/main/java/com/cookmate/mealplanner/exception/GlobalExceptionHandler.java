package com.cookmate.mealplanner.exception;

import com.cookmate.mealplanner.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(MealPlanGenerationException.class)
    public ResponseEntity<ErrorResponseDto> handleMealPlanGenerationException(
            MealPlanGenerationException exception
    ) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "MEAL_PLAN_GENERATION_FAILED",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(WeeklyPlanNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleWeeklyPlanNotFoundException(
            WeeklyPlanNotFoundException exception
    ) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "WEEKLY_PLAN_NOT_FOUND",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        ErrorResponseDto errorResponse = new ErrorResponseDto("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException exception) {
        ErrorResponseDto errorResponse = new ErrorResponseDto("VALIDATION_ERROR", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
