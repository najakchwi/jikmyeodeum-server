package com.sportsmate.server.common.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Email 값 객체 단위 테스트")
class EmailTest {

    @Test
    @DisplayName("올바른 형식의 이메일로 생성하면 value를 그대로 보존한다")
    void create_withValidFormat_preservesValue() {
        var email = new Email("user@example.com");

        assertThat(email.value()).isEqualTo("user@example.com");
        assertThat(email.toString()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("이메일이 null이면 예외가 발생한다")
    void create_withNull_throwsException() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("이메일이 공백이면 예외가 발생한다")
    void create_withBlank_throwsException() {
        assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("이메일에 앞뒤 공백이 있으면 예외가 발생한다")
    void create_withSurroundingWhitespace_throwsException() {
        assertThatThrownBy(() -> new Email(" user@example.com "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whitespace");
    }

    @Test
    @DisplayName("형식에 맞지 않는 이메일이면 예외가 발생한다")
    void create_withInvalidFormat_throwsException() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("동일한 value를 가진 Email은 동등하다")
    void equals_withSameValue_returnsTrue() {
        var email1 = new Email("user@example.com");
        var email2 = new Email("user@example.com");

        assertThat(email1).isEqualTo(email2);
    }
}
