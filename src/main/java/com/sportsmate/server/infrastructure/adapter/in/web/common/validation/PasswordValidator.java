package com.sportsmate.server.infrastructure.adapter.in.web.common.validation;

import com.sportsmate.server.domain.member.policy.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return PasswordPolicy.isBlank(value) || PasswordPolicy.isValid(value);
    }
}
