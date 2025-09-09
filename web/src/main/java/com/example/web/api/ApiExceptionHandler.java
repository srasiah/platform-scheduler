package com.example.web.api;

import com.example.scheduler.service.InternalServiceException;
import com.example.scheduler.service.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    record ErrorDto(String message) {}

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorDto> notFound(NotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorDto> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(InternalServiceException.class)
    ResponseEntity<ErrorDto> internal(InternalServiceException ex) {
        return ResponseEntity.internalServerError().body(new ErrorDto(ex.getMessage()));
    }
}
