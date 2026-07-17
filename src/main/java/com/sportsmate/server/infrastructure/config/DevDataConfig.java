package com.sportsmate.server.infrastructure.config;

import com.sportsmate.server.infrastructure.adapter.out.persistence.game.GameEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.GameJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.StadiumEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.StadiumJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.TeamEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.TeamJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.AvatarPresetEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.AvatarPresetJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.BannerEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.BannerJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.ContentAssetEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.ContentAssetJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.FaqEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.content.FaqJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.member.TermsEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.policy.TermsJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "test"})
public class DevDataConfig {

    @Bean
    CommandLineRunner seedGames(
            GameJpaRepository repository,
            TeamJpaRepository teamRepository,
            StadiumJpaRepository stadiumRepository) {
        return args -> {
            if (teamRepository.count() == 0) {
                // auto-increment IDs: LG=1, SSG=2, KIA=3, 삼성=4, 롯데=5, 한화=6, KT=7, 두산=8, NC=9, 키움=10
                teamRepository.saveAll(List.of(
                        team("LG 트윈스", "LG", "images/team-logo/lg.png", "#C30452"),
                        team("SSG 랜더스", "SSG", "images/team-logo/ssg.png", "#CE0E2D"),
                        team("KIA 타이거즈", "KIA", "images/team-logo/kia.png", "#EA0029"),
                        team("삼성 라이온즈", "삼성", "images/team-logo/samsung.png", "#074CA1"),
                        team("롯데 자이언츠", "롯데", "images/team-logo/lotte.png", "#041E42"),
                        team("한화 이글스", "한화", "images/team-logo/hanwha.png", "#FC4E00"),
                        team("KT 위즈", "KT", "images/team-logo/kt.png", "#000000"),
                        team("두산 베어스", "두산", "images/team-logo/doosan.png", "#131230"),
                        team("NC 다이노스", "NC", "images/team-logo/nc.png", "#315288"),
                        team("키움 히어로즈", "키움", "images/team-logo/kiwoom.png", "#570514")));
            }
            if (stadiumRepository.count() == 0) {
                stadiumRepository.saveAll(List.of(
                        stadium(1L, "잠실야구장"),
                        stadium(2L, "인천 SSG랜더스필드"),
                        stadium(3L, "광주-기아 챔피언스 필드"),
                        stadium(4L, "대구 삼성라이온즈파크"),
                        stadium(5L, "부산 사직야구장"),
                        stadium(6L, "대전 한화생명 볼파크"),
                        stadium(7L, "수원 KT위즈파크")));
            }
            if (repository.count() > 0) return;
            LocalDate today = LocalDate.now();
            repository.saveAll(List.of(
                    game(1L, 8L, 1L, today.plusDays(3)),   // LG vs 두산, 잠실
                    game(2L, 9L, 2L, today.plusDays(3)),   // SSG vs NC, 인천
                    game(3L, 6L, 3L, today.plusDays(4)),   // KIA vs 한화, 광주
                    game(4L, 10L, 4L, today.plusDays(4)),  // 삼성 vs 키움, 대구
                    game(5L, 7L, 5L, today.plusDays(5)),   // 롯데 vs KT, 부산
                    game(6L, 9L, 6L, today.plusDays(6)),   // 한화 vs NC, 대전
                    game(7L, 3L, 7L, today.plusDays(7)))); // KT vs KIA, 수원
        };
    }

    @Bean
    CommandLineRunner seedPolicyAndContent(
            TermsJpaRepository termsRepository,
            BannerJpaRepository bannerRepository,
            FaqJpaRepository faqRepository,
            AvatarPresetJpaRepository avatarPresetRepository,
            ContentAssetJpaRepository contentAssetRepository) {
        return args -> {
            if (termsRepository.count() == 0) {
                LocalDateTime effectiveAt = LocalDateTime.of(2026, 6, 21, 0, 0);
                termsRepository.saveAll(List.of(
                        terms("service", "2026-06-21", "이용약관",
                                "레츠포츠 서비스 이용 조건과 회원의 권리 및 의무를 규정합니다.", true, null, effectiveAt),
                        terms("privacy", "2026-06-21", "개인정보처리방침",
                                "회원 식별과 매칭 서비스 제공을 위해 필요한 개인정보 처리 기준을 안내합니다.", true, null, effectiveAt),
                        terms("location", "2026-06-21", "위치기반서비스 이용약관",
                                "거리 기반 매칭과 위치 인증을 위한 위치기반서비스 이용 조건을 규정합니다.", true, null, effectiveAt),
                        terms("age14", "2026-06-21", "만 14세 이상 확인",
                                "레츠포츠는 만 14세 이상만 가입할 수 있으며, 가입자는 만 14세 이상임을 확인합니다.", true, null, effectiveAt),
                        terms("marketing", "2026-06-21", "마케팅 정보 수신 동의",
                                "이벤트, 혜택, 서비스 소식 등 마케팅 정보를 앱 푸시 또는 알림으로 받을 수 있습니다.", false, 365, effectiveAt)));
            }
            if (bannerRepository.count() == 0) {
                bannerRepository.saveAll(List.of(
                        banner("home_main_1", "직관 메이트 찾기", "images/banners/home-main-1.png", 1),
                        banner("home_main_2", "응원 스타일 매칭", "images/banners/home-main-2.png", 2)));
            }
            if (faqRepository.count() == 0) {
                faqRepository.saveAll(List.of(
                        faq("matching_flow", "matching", "매칭은 어떻게 진행되나요?",
                                "같은 경기에 신청한 사용자 중 응원팀, 관람 스타일, 성격, 매칭 허용 거리, 신뢰도 점수 등을 종합적으로 고려해 매칭합니다. 상대가 배치되면 채팅방이 자동으로 열립니다.", 1),
                        faq("cancel_application", "matching", "신청한 매칭을 취소할 수 있나요?",
                                "상대 배치 전에는 불이익 없이 취소할 수 있습니다. 다만 매칭이 확정된 이후 정당한 사유 없이 반복적으로 취소하면 신뢰도 점수가 차감될 수 있습니다.", 2),
                        faq("report_user", "safety", "신고는 어떻게 하나요?",
                                "채팅방 또는 매칭 결과 화면 우측 메뉴에서 신고 사유를 선택해 접수할 수 있습니다. 접수된 신고는 운영팀 검토 후 처리됩니다.", 3)));
            }
            if (avatarPresetRepository.count() == 0) {
                avatarPresetRepository.saveAll(List.of(
                        avatar("baseball_cap_green", "초록 야구모자", "images/avatars/presets/baseball-cap-green.png", 1),
                        avatar("baseball_cap_red", "빨강 야구모자", "images/avatars/presets/baseball-cap-red.png", 2)));
            }
            if (contentAssetRepository.count() == 0) {
                contentAssetRepository.saveAll(List.of(
                        asset("icon", "match", "매칭 아이콘", "images/icons/match.png", 1),
                        asset("general", "default_avatar", "기본 아바타", "images/general/default-avatar.png", 1)));
            }
        };
    }

    private GameEntity game(Long homeTeamId, Long awayTeamId, Long stadiumId, LocalDate date) {
        return GameEntity.builder()
                .leagueId(1L)
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .stadiumId(stadiumId)
                .date(date)
                .time(LocalTime.of(18, 30))
                .deadline(date.minusDays(1))
                .status("scheduled")
                .build();
    }

    private TeamEntity team(String name, String shortName, String emblemImageKey, String primaryColorHex) {
        return TeamEntity.builder()
                .sportId(1L)
                .leagueId(1L)
                .name(name)
                .shortName(shortName)
                .emblemImageKey(emblemImageKey)
                .primaryColorHex(primaryColorHex)
                .build();
    }

    private StadiumEntity stadium(Long id, String name) {
        return StadiumEntity.builder()
                .name(name)
                .build();
    }

    private TermsEntity terms(String code, String version, String title, String content,
            boolean required, Integer validDays, LocalDateTime effectiveAt) {
        return TermsEntity.builder()
                .code(code)
                .version(version)
                .title(title)
                .content(content)
                .required(required)
                .validDays(validDays)
                .effectiveAt(effectiveAt)
                .build();
    }

    private BannerEntity banner(String code, String title, String imageKey, int displayOrder) {
        return BannerEntity.builder()
                .code(code)
                .title(title)
                .imageKey(imageKey)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }

    private FaqEntity faq(String code, String category, String question, String answer, int displayOrder) {
        return FaqEntity.builder()
                .code(code)
                .category(category)
                .question(question)
                .answer(answer)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }

    private AvatarPresetEntity avatar(String code, String name, String imageKey, int displayOrder) {
        return AvatarPresetEntity.builder()
                .code(code)
                .name(name)
                .imageKey(imageKey)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }

    private ContentAssetEntity asset(String type, String code, String name, String objectKey, int displayOrder) {
        return ContentAssetEntity.builder()
                .type(type)
                .code(code)
                .name(name)
                .objectKey(objectKey)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }
}
