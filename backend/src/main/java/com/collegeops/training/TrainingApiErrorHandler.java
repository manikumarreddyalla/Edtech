package com.collegeops.training;

import com.collegeops.common.ApiErrorResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrainingApiErrorHandler {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidQueryParameter(MethodArgumentTypeMismatchException exception) {
        var response = new ApiErrorResponse("VALIDATION_ERROR", "One or more query parameters are invalid.",
                List.of(), UUID.randomUUID().toString());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(TrainingRequestException.class)
    public ResponseEntity<ApiErrorResponse> handle(TrainingRequestException exception) {
        var response = new ApiErrorResponse(exception.getCode(), exception.getMessage(), List.of(),
                UUID.randomUUID().toString());
        return ResponseEntity.status(HttpStatusCode.valueOf(exception.getStatus())).body(response);
    }
}
