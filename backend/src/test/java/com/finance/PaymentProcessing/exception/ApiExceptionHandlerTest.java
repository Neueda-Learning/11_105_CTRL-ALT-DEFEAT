package com.finance.PaymentProcessing.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFound_returns404WithErrorCode() {
        ResponseEntity<Map<String, Object>> response =
                handler.notFound(new NotFoundException("CUSTOM_CODE", "not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("CUSTOM_CODE", response.getBody().get("errorCode"));
        assertEquals("not found", response.getBody().get("message"));
    }

    @Test
    void bad_returns400WithErrorCode() {
        ResponseEntity<Map<String, Object>> response =
                handler.bad(new BadRequestException("BAD_CODE", "bad request"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_CODE", response.getBody().get("errorCode"));
    }

    @Test
    void conflict_returns409WithErrorCode() {
        ResponseEntity<Map<String, Object>> response =
                handler.conflict(new ConflictException("CONFLICT_CODE", "conflict"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("CONFLICT_CODE", response.getBody().get("errorCode"));
    }

    @Test
    void validation_usesFirstFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "amount", "must be positive");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.validation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_FAILED", response.getBody().get("errorCode"));
        assertEquals("amount: must be positive", response.getBody().get("message"));
    }

    @Test
    void validation_noFieldErrors_usesDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

        ResponseEntity<Map<String, Object>> response = handler.validation(ex);

        assertEquals("Invalid request", response.getBody().get("message"));
    }
}
