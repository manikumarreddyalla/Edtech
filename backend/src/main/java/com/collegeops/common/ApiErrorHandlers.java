package com.collegeops.common;

import com.collegeops.security.InvalidCredentialsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class ApiErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandlers.class);
    private final ObjectMapper objectMapper;

    public ApiErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not permitted to perform this action.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public void handleInvalidCredentials(HttpServletResponse response) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "The supplied credentials are invalid.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidation(HttpServletResponse response, MethodArgumentNotValidException exception)
            throws IOException {
        List<FieldErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        writeError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "One or more fields are invalid.", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void handleMalformedRequest(HttpServletResponse response) throws IOException {
        writeError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request body is malformed.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public void handleConflict(HttpServletResponse response) throws IOException {
        writeError(response, HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data.");
    }

    @ExceptionHandler(Exception.class)
    public void handleUnexpected(HttpServletResponse response, Exception exception) throws IOException {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled request failure, traceId={}", traceId, exception);
        writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", List.of(), traceId);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        writeError(response, status, code, message, List.of());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message,
                            List<FieldErrorDetail> details) throws IOException {
        writeError(response, status, code, message, details, UUID.randomUUID().toString());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message,
                            List<FieldErrorDetail> details, String traceId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(code, message, details, traceId));
    }
}
