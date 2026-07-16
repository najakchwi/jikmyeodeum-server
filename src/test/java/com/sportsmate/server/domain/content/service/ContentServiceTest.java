package com.sportsmate.server.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import com.sportsmate.server.domain.content.AvatarPreset;
import com.sportsmate.server.domain.content.BannerContent;
import com.sportsmate.server.domain.content.ContentAsset;
import com.sportsmate.server.domain.content.FaqContent;
import com.sportsmate.server.domain.content.port.out.ContentOutPort;
import com.sportsmate.server.domain.game.Team;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContentService 단위 테스트")
class ContentServiceTest {

    private final ContentService contentService = new ContentService(
            new EmptyContentOutPort(),
            new EmptyTeamOutPort(),
            new StubObjectStorage());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("bootstrap 프로필 옵션은 비활성 옵션을 제외하고 선택 정책을 포함한다")
    void getBootstrapContent_profileOptions_excludesInactiveAndIncludesSelectionPolicy() throws Exception {
        setActive(WatchStyle.FOOD, false);
        try {
            var profileOptions = contentService.getBootstrapContent().profileOptions();

            assertThat(profileOptions.watchStyles().options())
                    .extracting(option -> option.value())
                    .doesNotContain(WatchStyle.FOOD.value());
            assertThat(profileOptions.watchStyles().multiSelectable()).isTrue();
            assertThat(profileOptions.watchStyles().maxCount()).isEqualTo(2);
            assertThat(profileOptions.personalities().multiSelectable()).isFalse();
            assertThat(profileOptions.personalities().maxCount()).isNull();
            assertThat(profileOptions.talkStyles().multiSelectable()).isFalse();
            assertThat(profileOptions.talkStyles().maxCount()).isNull();
            assertThat(profileOptions.smokingStatuses().multiSelectable()).isFalse();
            assertThat(profileOptions.smokingStatuses().maxCount()).isNull();
        } finally {
            setActive(WatchStyle.FOOD, true);
        }
    }

    @Test
    @DisplayName("bootstrap 프로필 옵션 선택 정책은 중첩 객체로 직렬화된다")
    void getBootstrapContent_profileOptions_serializesPolicyAsNestedGroup() {
        JsonNode root = objectMapper.valueToTree(contentService.getBootstrapContent());
        JsonNode profileOptions = root.path("profileOptions");
        JsonNode watchStyles = profileOptions.path("watchStyles");

        assertThat(watchStyles.isObject()).isTrue();
        assertThat(watchStyles.path("options").isArray()).isTrue();
        assertThat(watchStyles.path("multiSelectable").asBoolean()).isTrue();
        assertThat(watchStyles.path("maxCount").asInt()).isEqualTo(2);
        assertThat(profileOptions.has("watchStylesMultiSelectable")).isFalse();
        assertThat(profileOptions.has("watchStylesMaxCount")).isFalse();
        assertThat(profileOptions.path("genderPrefs").isArray()).isTrue();
        assertThat(profileOptions.path("agePrefs").isArray()).isTrue();
        assertThat(profileOptions.path("smokingPrefs").isArray()).isTrue();
    }

    private static void setActive(Enum<?> option, boolean active) throws Exception {
        Field field = option.getClass().getDeclaredField("active");
        field.setAccessible(true);
        field.set(option, active);
    }

    private static class EmptyContentOutPort implements ContentOutPort {
        @Override
        public List<BannerContent> findActiveBanners(LocalDateTime now) {
            return List.of();
        }

        @Override
        public List<FaqContent> findActiveFaqs() {
            return List.of();
        }

        @Override
        public List<AvatarPreset> findActiveAvatarPresets() {
            return List.of();
        }

        @Override
        public List<ContentAsset> findActiveAssets() {
            return List.of();
        }
    }

    private static class EmptyTeamOutPort implements TeamOutPort {
        @Override
        public List<Team> findAll() {
            return List.of();
        }

        @Override
        public Optional<Team> findByKboCode(String kboCode) {
            return Optional.empty();
        }

        @Override
        public Optional<Team> findByShortName(String shortName) {
            return Optional.empty();
        }
    }

    private static class StubObjectStorage implements ObjectStorage {
        @Override
        public StoredObject upload(ObjectUploadCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String objectKey) {
        }

        @Override
        public Optional<byte[]> download(String objectKey) {
            return Optional.empty();
        }

        @Override
        public String getUrl(String objectKey) {
            return "https://cdn.example.com/" + objectKey;
        }

        @Override
        public String extractKey(String url) {
            return null;
        }
    }
}
