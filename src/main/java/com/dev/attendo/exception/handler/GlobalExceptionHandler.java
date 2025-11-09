package com.dev.attendo.exception.handler;

import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.security.response.MessageResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException exception) {
        return new ResponseEntity<>(new MessageResponse(false, exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> badRequestException(BadRequestException exception) {
        return new ResponseEntity<>(new MessageResponse(false, exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<?> internalServerErrorException(InternalServerErrorException exception) {
        return new ResponseEntity<>(new MessageResponse(false, exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation error");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(false, errorMessage));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(false, String.format("Data '%s' is on wrong type", exception.getName())));
    }
}
