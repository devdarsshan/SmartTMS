package com.smartlogistics.auth.validator;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[@$!%*?&].*";

    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return new PasswordValidationResult(false, errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (!Pattern.matches(UPPERCASE_PATTERN, password)) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!Pattern.matches(LOWERCASE_PATTERN, password)) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!Pattern.matches(DIGIT_PATTERN, password)) {
            errors.add("Password must contain at least one digit");
        }

        if (!Pattern.matches(SPECIAL_CHAR_PATTERN, password)) {
            errors.add("Password must contain at least one special character (@$!%*?&)");
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}

