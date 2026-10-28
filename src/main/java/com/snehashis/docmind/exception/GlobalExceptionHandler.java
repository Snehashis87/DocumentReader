package com.snehashis.docmind.exception;

import com.snehashis.docmind.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());

    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiResponse<Object>> handleDocumentProcessing(DocumentProcessingException ex) {
        logger.error("Document processing error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ApiResponse.
                        builder()
                        .success(false)
                        .message("Failed to process document: " +ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ApiResponse
                        .builder()
                        .success(false)
                        .message("File size exceeds the maximum limit of 25MB.")
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }


        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed for one or more fields.")
                        .data(errors)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());


    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse
                        .builder()
                        .success(false)
                        .message("Invalid input: " + ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse
                        .builder()
                        .success(false)
                        .message("The requested data conflicts with an existing record.")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // Generalized exception handler for any other exceptions not explicitly handled
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {

        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message("An unexpected error occurred: " + ex.getMessage())
                                .data(null)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }
}
