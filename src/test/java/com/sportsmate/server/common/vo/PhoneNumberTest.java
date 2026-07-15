package com.sportsmate.server.common.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PhoneNumber 값 객체 단위 테스트")
class PhoneNumberTest {

    @Test
    @DisplayName("올바른 형식의 전화번호로 생성하면 value를 그대로 보존한다")
    void create_withValidFormat_preservesValue() {
        var phoneNumber = new PhoneNumber("01012345678");

        assertThat(phoneNumber.value()).isEqualTo("01012345678");
        assertThat(phoneNumber.toString()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("'-' 구분자가 포함된 전화번호는 정규화하여 숫자만 저장한다")
    void create_withHyphens_normalizesValue() {
        var phoneNumber = new PhoneNumber("010-1234-5678");

        assertThat(phoneNumber.value()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("전화번호가 null이면 예외가 발생한다")
    void create_withNull_throwsException() {
        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("전화번호가 공백이면 예외가 발생한다")
    void create_withBlank_throwsException() {
        assertThatThrownBy(() -> new PhoneNumber("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("형식에 맞지 않는 전화번호이면 예외가 발생한다")
    void create_withInvalidFormat_throwsException() {
        assertThatThrownBy(() -> new PhoneNumber("0212345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("동일한 value를 가진 PhoneNumber는 동등하다")
    void equals_withSameValue_returnsTrue() {
        var phoneNumber1 = new PhoneNumber("01012345678");
        var phoneNumber2 = new PhoneNumber("010-1234-5678");

        assertThat(phoneNumber1).isEqualTo(phoneNumber2);
    }
}
