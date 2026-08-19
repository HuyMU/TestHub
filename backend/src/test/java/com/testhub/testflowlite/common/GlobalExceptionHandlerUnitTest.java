package com.testhub.testflowlite.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleGenericException_SanitizesMessageAndDoesNotLeakDetails() {
        RuntimeException sensitiveException = new RuntimeException("SQLSTATE[28000]: Access denied for user 'root'@'localhost' (using password: YES) - sensitive database credentials");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(sensitiveException);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());

        String message = response.getBody().getMessage();
        assertFalse(message.contains("SQLSTATE"));
        assertFalse(message.contains("sensitive"));
        assertFalse(message.contains("root"));
        assertFalse(message.contains("localhost"));
        assertEquals("An unexpected error occurred. Please try again or contact support.", message);
    }

    @Test
    void testHandleNotFound_ReturnsNotFoundWithProvidedMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Project not found: 42");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Project not found: 42", response.getBody().getMessage());
    }

    @Test
    void testHandleForbidden_ReturnsForbiddenWithProvidedMessage() {
        ForbiddenException ex = new ForbiddenException("You do not have access to this project");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleForbidden(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You do not have access to this project", response.getBody().getMessage());
    }
}
