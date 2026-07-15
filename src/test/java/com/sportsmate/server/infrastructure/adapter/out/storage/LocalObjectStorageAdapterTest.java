package com.sportsmate.server.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LocalObjectStorageAdapter 단위 테스트")
class LocalObjectStorageAdapterTest {

    private final LocalObjectStorageAdapter adapter = new LocalObjectStorageAdapter("http://localhost:8080");

    @Test
    @DisplayName("getUrl로 만든 URL에서 extractKey로 objectKey를 다시 추출할 수 있다")
    void extractKey_roundTripsWithGetUrl() {
        String url = adapter.getUrl("avatars/1/profile.png");

        assertThat(adapter.extractKey(url)).isEqualTo("avatars/1/profile.png");
    }

    @Test
    @DisplayName("형식에 맞지 않는 URL이면 extractKey는 null을 반환한다")
    void extractKey_unknownFormat_returnsNull() {
        assertThat(adapter.extractKey("https://cdn.example.com/avatars/1/profile.png")).isNull();
        assertThat(adapter.extractKey(null)).isNull();
    }
}
