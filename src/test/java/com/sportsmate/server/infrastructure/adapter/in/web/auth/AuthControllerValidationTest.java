package com.sportsmate.server.infrastructure.adapter.in.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthController 요청 DTO 검증 테스트")
class AuthControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("탈퇴 사유가 OTHER인데 상세 사유가 없으면 검증 실패한다")
    void withdrawRequest_otherWithoutReasonDetail_invalid() {
        var request = new AuthController.WithdrawRequest(WithdrawalReason.OTHER, null);

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("reasonDetailValid"));
    }

    @Test
    @DisplayName("탈퇴 사유가 OTHER가 아니면 상세 사유 없이도 검증에 성공한다")
    void withdrawRequest_nonOtherWithoutReasonDetail_valid() {
        var request = new AuthController.WithdrawRequest(WithdrawalReason.LOW_USAGE, null);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
