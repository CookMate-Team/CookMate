package com.cookmate.main.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Global exception handler for REST endpoints.
 * Handles validation errors and other application exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors.
     *
     * @param ex MethodArgumentNotValidException with validation failures
     * @return error response with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = Stream.concat(
            ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage())),
            ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ApiErrorDetail(error.getObjectName(), error.getDefaultMessage()))
        ).toList();

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.REQUEST_VALIDATION_FAILED,
            "Request validation failed",
            details,
            request
        );
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            logger.error("API error: {} [{}]", ex.getMessage(), ex.getErrorCode(), ex);
        } else {
            logger.warn("API error: {} [{}]", ex.getMessage(), ex.getErrorCode());
        }

        return buildErrorResponse(
            ex.getStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            List.of(),
            request
        );
    }

    /**
     * Handle bean validation constraint violations on request parameters.
     *
     * @param ex ConstraintViolationException with constraint failures
     * @return error response with constraint details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = ex.getConstraintViolations().stream()
            .map(cv -> new ApiErrorDetail(cv.getPropertyPath().toString(), cv.getMessage()))
            .toList();

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.REQUEST_VALIDATION_FAILED,
            "Request validation failed",
            details,
            request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameterException(
        MissingServletRequestParameterException ex,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail(ex.getParameterName(), "Parameter is required"));
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.REQUEST_VALIDATION_FAILED,
            ex.getMessage(),
            details,
            request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        String detailMessage = "Invalid value. Expected type: " + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown") 
            + ". Provided: " + (ex.getValue() == null ? "null" : ex.getValue().toString());
        List<ApiErrorDetail> details = List.of(
            new ApiErrorDetail(ex.getName(), detailMessage)
        );

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.ARGUMENT_TYPE_MISMATCH,
            message,
            details,
            request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.REQUEST_BODY_INVALID,
            "Malformed request body",
            List.of(),
            request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            ErrorCode.METHOD_NOT_ALLOWED,
            ex.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.MEDIA_TYPE_NOT_SUPPORTED,
            ex.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.REQUEST_VALIDATION_FAILED,
            ex.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof ApiException apiException) {
            logger.warn("Resolved wrapped API exception: {} [{}]", apiException.getMessage(), apiException.getErrorCode());
            return buildErrorResponse(
                apiException.getStatus(),
                apiException.getErrorCode(),
                apiException.getMessage(),
                List.of(),
                request
            );
        }

        logger.error("Unhandled exception", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            List.of(),
            request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
        HttpStatus status,
        ErrorCode code,
        String message,
        List<ApiErrorDetail> details,
        HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code.name(),
            message,
            request.getRequestURI(),
            traceId,
            details
        );

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Trace-Id", traceId)
            .body(errorResponse);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Request-Id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return sanitizeTraceId(traceId);
    }

    private String sanitizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        // Max length: 256 chars (generous for various ID formats)
        if (traceId.length() > 256) {
            traceId = traceId.substring(0, 256);
        }
        // Remove control characters (CR, LF, null, etc.) and other dangerous chars for headers/logs
        traceId = traceId.replaceAll("[\\r\\n\\t\\u0000-\\u001F\\u007F]", "");
        // Keep only alphanumeric, hyphen, underscore, colon, dot, slash (standard UUID/trace ID chars)
        if (!traceId.matches("[a-zA-Z0-9\\-_:./@]*")) {
            traceId = traceId.replaceAll("[^a-zA-Z0-9\\-_:./@]", "");
        }
        // If sanitization resulted in empty string, generate new UUID
        if (traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
