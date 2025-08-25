package com.morago.backend.controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ---------- 400: malformed JSON / wrong field types ---------- */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String msg = "Malformed JSON request";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            msg = "Invalid value for field: " + ife.getPathReference();
        }
        return build(HttpStatus.BAD_REQUEST, msg, req, null);
    }

    /* ---------- 400: @Valid on request body (DTO) ---------- */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(err -> errors.put(err.getObjectName(), err.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }

    /* ---------- 400: binding query/path parameters ---------- */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBind(BindException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Binding failed", req, errors);
    }

    /* ---------- 400: method parameter validation (@Validated) ---------- */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Constraint violation", req, errors);
    }

    /* ---------- 400: parameter type mismatch ---------- */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, msg, req, null);
    }

    /* ---------- 401 ---------- */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", req, null);
    }

    /* ---------- 403 ---------- */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", req, null);
    }

    /* ---------- 404: your custom not-found exceptions ---------- */
    @ExceptionHandler({UserNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /* ---------- 404: no handler (enable properties below if needed) ---------- */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No handler for " + ex.getRequestURL(), req, null);
    }

    /* ---------- 409: unique constraints, etc. ---------- */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        Throwable root = ex.getMostSpecificCause();

        String lower = "";
        String text = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        if (text != null) lower = text.toLowerCase();

        String constraint = "";
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException cve
                && cve.getConstraintName() != null) {
            constraint = cve.getConstraintName().toLowerCase();
        }

        // Special case: duplicate phone/username
        if ((lower.contains("duplicate") || lower.contains("unique"))
                && (lower.contains("phone") || lower.contains("phone_number") || lower.contains("username")
                || constraint.contains("phone") || constraint.contains("username"))) {
            return build(HttpStatus.CONFLICT, "Phone is already registered", req, null, "PHONE_TAKEN");
        }

        // Otherwise — generic conflict message
        String msg = resolveDataIntegrityMessage(ex);
        return build(HttpStatus.CONFLICT, msg, req, null);
    }

    /* ---------- 400: business validation ---------- */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /* ---------- 500: everything else ---------- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req, null);
    }

    @ExceptionHandler({com.morago.backend.exception.ExpireJwtTokenException.class,
            com.morago.backend.exception.InvalidJwtTokenException.class})
    public ResponseEntity<Object> handleJwt(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing parameter: " + ex.getParameterName(), req, null);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<Object> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing header: " + ex.getHeaderName(), req, null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", req, null);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", req, null);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUpload(org.springframework.web.multipart.MaxUploadSizeExceededException ex,
                                                  HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Payload too large", req, null);
    }

    @ExceptionHandler(com.morago.backend.exception.ApiException.class)
    public ResponseEntity<Object> handleApi(com.morago.backend.exception.ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, ex.getErrors(), ex.getCode());
    }


    /* ================= helpers ================= */

    private ResponseEntity<Object> build(HttpStatus status, String message,
                                         HttpServletRequest req, Map<String, String> errors) {
        return build(status, message, req, errors, null);
    }

    /** Try to identify the cause of the conflict (unique/fk) across different DBs/drivers */
    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException h) {
            String name = h.getConstraintName();
            if (name != null && name.toLowerCase().contains("unique")) {
                return "Duplicate value (unique constraint)";
            }
        }

        Throwable root = ex.getMostSpecificCause();

        if (root instanceof java.sql.SQLException sql) {
            String state = sql.getSQLState();
            if ("23505".equals(state) || "23000".equals(state) || sql.getErrorCode() == 1062) {
                return "Duplicate value (unique constraint)";
            }
            if ("23503".equals(state) || sql.getErrorCode() == 1452) {
                return "Referential integrity violation (foreign key)";
            }
        }

        String text = (root.getMessage() != null) ? root.getMessage() : ex.getMessage();
        if (text != null) {
            String l = text.toLowerCase();
            if (l.contains("unique") || l.contains("duplicate")) {
                return "Duplicate value (unique constraint)";
            }
            if (l.contains("foreign key")) {
                return "Referential integrity violation (foreign key)";
            }
        }
        return "Conflict";
    }

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String code;
        private String path;
        private Instant timestamp;
        private Map<String, String> errors;
    }

    private ResponseEntity<Object> build(HttpStatus status, String message,
                                         HttpServletRequest req, Map<String, String> errors, String code) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                code,
                req.getRequestURI(),
                Instant.now(),
                errors
        );
        return ResponseEntity.status(status).body(body);
    }
}