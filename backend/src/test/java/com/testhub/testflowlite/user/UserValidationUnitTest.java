package com.testhub.testflowlite.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationUnitTest {

    private static Validator validator;
    private static final String COMPLEXITY_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, and one digit";

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testCreateUserRequest_ValidPassword_NoViolations() {
        CreateUserRequest request = new CreateUserRequest("tester1", "tester1@testhub.com", "ValidPass1", "Tester One");
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testCreateUserRequest_PasswordTooShort_Violations() {
        CreateUserRequest request = new CreateUserRequest("tester1", "tester1@testhub.com", "Short1A", "Tester One");
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 8 characters")));
    }

    @Test
    void testCreateUserRequest_PasswordNoUppercase_Violations() {
        CreateUserRequest request = new CreateUserRequest("tester1", "tester1@testhub.com", "alllowercase1", "Tester One");
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(COMPLEXITY_MESSAGE)));
    }

    @Test
    void testCreateUserRequest_PasswordNoDigit_Violations() {
        CreateUserRequest request = new CreateUserRequest("tester1", "tester1@testhub.com", "NoDigitsHere", "Tester One");
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(COMPLEXITY_MESSAGE)));
    }

    @Test
    void testCreateUserRequest_PasswordNoLowercase_Violations() {
        CreateUserRequest request = new CreateUserRequest("tester1", "tester1@testhub.com", "ALLUPPERCASE1", "Tester One");
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(COMPLEXITY_MESSAGE)));
    }

    @Test
    void testChangePasswordRequest_ValidNewPassword_NoViolations() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "NewSecurePass1");
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testChangePasswordRequest_NewPasswordTooShort_Violations() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "Weak1A");
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 8 characters")));
    }

    @Test
    void testChangePasswordRequest_NewPasswordNoComplexity_Violations() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "weakpasswords");
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(COMPLEXITY_MESSAGE)));
    }
}
