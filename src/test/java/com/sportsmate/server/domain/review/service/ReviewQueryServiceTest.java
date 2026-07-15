package com.sportsmate.server.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.review.port.in.ReviewQueryUseCase;
import com.sportsmate.server.domain.review.port.out.ReviewQueryOutPort;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewQueryService 단위 테스트")
class ReviewQueryServiceTest {
    private final FakeReviewQueryOutPort reviewQueryOutPort = new FakeReviewQueryOutPort();
    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final ReviewQueryService reviewQueryService =
            new ReviewQueryService(reviewQueryOutPort, memberOutPort);

    @Test
    @DisplayName("받은 리뷰 요약과 별점 분포는 1~5점 전 구간을 포함한다")
    void getReceivedReviews_withReviews_returnsSummaryAndFullDistribution() {
        memberOutPort.members.put(1L, member(1L, 4.64));
        reviewQueryOutPort.count = 32;
        reviewQueryOutPort.distribution.put(5, 24L);
        reviewQueryOutPort.distribution.put(4, 6L);
        reviewQueryOutPort.distribution.put(3, 1L);
        reviewQueryOutPort.distribution.put(2, 1L);

        ReviewQueryUseCase.ReceivedReviewsResult result =
                reviewQueryService.getReceivedReviews(1L, null, 20);

        assertThat(result.summary().average()).isEqualTo(4.6);
        assertThat(result.summary().count()).isEqualTo(32);
        assertThat(result.distribution()).containsExactly(
                Map.entry(5, 24L),
                Map.entry(4, 6L),
                Map.entry(3, 1L),
                Map.entry(2, 1L),
                Map.entry(1, 0L));
    }

    @Test
    @DisplayName("태그는 포트에서 받은 집계 순서를 유지한다")
    void getReceivedReviews_withTagCounts_keepsTagOrder() {
        memberOutPort.members.put(1L, member(1L, 0.0));
        reviewQueryOutPort.tags = List.of(
                new ReviewQueryOutPort.TagCount("또 만나고 싶어요", 18),
                new ReviewQueryOutPort.TagCount("시간 약속을 잘 지켜요", 12));

        ReviewQueryUseCase.ReceivedReviewsResult result =
                reviewQueryService.getReceivedReviews(1L, null, 20);

        assertThat(result.tags()).containsExactly(
                new ReviewQueryUseCase.TagCount("또 만나고 싶어요", 18),
                new ReviewQueryUseCase.TagCount("시간 약속을 잘 지켜요", 12));
    }

    @Test
    @DisplayName("코멘트 목록은 size 초과 조회로 다음 커서를 계산한다")
    void getReceivedReviews_withComments_returnsPageAndNextCursor() {
        memberOutPort.members.put(1L, member(1L, 5.0));
        reviewQueryOutPort.comments = List.of(
                comment(82L, 5, "매너가 좋았어요"),
                comment(61L, 4, "응원이 즐거웠어요"),
                comment(40L, 5, "다음에도 같이 가고 싶어요"));

        ReviewQueryUseCase.ReceivedReviewsResult result =
                reviewQueryService.getReceivedReviews(1L, "rv_99", 2);

        assertThat(reviewQueryOutPort.requestedCursor).isEqualTo("rv_99");
        assertThat(reviewQueryOutPort.requestedSize).isEqualTo(3);
        assertThat(result.comments().hasNext()).isTrue();
        assertThat(result.comments().nextCursor()).isEqualTo("rv_61");
        assertThat(result.comments().items())
                .extracting(ReviewQueryUseCase.ReviewComment::id)
                .containsExactly("rv_82", "rv_61");
    }

    @Test
    @DisplayName("리뷰가 없는 회원도 빈 응답 형태를 유지한다")
    void getReceivedReviews_withoutReviews_returnsEmptyState() {
        memberOutPort.members.put(1L, member(1L, 0.0));

        ReviewQueryUseCase.ReceivedReviewsResult result =
                reviewQueryService.getReceivedReviews(1L, null, 20);

        assertThat(result.summary().count()).isZero();
        assertThat(result.distribution()).containsOnly(
                Map.entry(5, 0L),
                Map.entry(4, 0L),
                Map.entry(3, 0L),
                Map.entry(2, 0L),
                Map.entry(1, 0L));
        assertThat(result.tags()).isEmpty();
        assertThat(result.comments().items()).isEmpty();
        assertThat(result.comments().hasNext()).isFalse();
        assertThat(result.comments().nextCursor()).isNull();
    }

    @Test
    @DisplayName("코멘트 응답에는 작성자 식별 필드가 없다")
    void reviewComment_doesNotExposeReviewerId() {
        assertThat(ReviewQueryUseCase.ReviewComment.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("reviewerId", "reviewer", "writerId", "authorId");
    }

    private ReviewQueryOutPort.ReceivedReviewComment comment(Long id, int rating, String comment) {
        return new ReviewQueryOutPort.ReceivedReviewComment(
                id, rating, List.of("또 만나고 싶어요"), comment, LocalDateTime.now().minusDays(id));
    }

    private Member member(Long id, double rating) {
        return Member.reconstitute(
                id,
                "0100000000" + id,
                "password",
                LoginType.PHONE,
                null,
                "회원" + id,
                "소개",
                LocalDate.of(1997, 1, 1),
                Gender.MALE,
                null,
                "#2E7D32",
                "LG",
                List.of(WatchStyle.CHEER),
                Personality.TENSION,
                TalkStyle.TALKATIVE,
                SmokingStatus.NON_SMOKER,
                GenderPref.ANY,
                AgePref.ANY,
                SmokingPref.ANY,
                5,
                true,
                "서울 송파구 잠실동",
                37.5,
                127.0,
                0,
                rating,
                100,
                0,
                0,
                false,
                Role.USER);
    }

    private static class FakeReviewQueryOutPort implements ReviewQueryOutPort {
        private long count;
        private final Map<Integer, Long> distribution = new LinkedHashMap<>();
        private List<TagCount> tags = List.of();
        private List<ReceivedReviewComment> comments = List.of();
        private String requestedCursor;
        private int requestedSize;

        @Override
        public long countByTargetMemberId(Long targetMemberId) {
            return count;
        }

        @Override
        public Map<Integer, Long> ratingDistribution(Long targetMemberId) {
            return distribution;
        }

        @Override
        public List<TagCount> tagCounts(Long targetMemberId) {
            return tags;
        }

        @Override
        public Optional<ReviewCursor> findCursor(String cursor) {
            requestedCursor = cursor;
            return Optional.of(new ReviewCursor(99L, LocalDateTime.now()));
        }

        @Override
        public List<ReceivedReviewComment> comments(Long targetMemberId, ReviewCursor cursor, int size) {
            requestedSize = size;
            return comments;
        }
    }

    private static class FakeMemberOutPort implements MemberOutPort {
        private final Map<Long, Member> members = new LinkedHashMap<>();

        @Override
        public Member save(Member member) {
            members.put(member.getId(), member);
            return member;
        }

        @Override
        public Optional<Member> findById(Long id) {
            return Optional.ofNullable(members.get(id));
        }

        @Override
        public Optional<Member> findByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByProvider(LoginType loginType, String providerId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findExpoPushTokenById(Long id) {
            return Optional.empty();
        }

        @Override
        public boolean isWelcomeNotified(Long id) {
            return false;
        }

        @Override
        public boolean existsByPhone(String phone) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public void updateExpoPushToken(Long id, String expoPushToken) {
        }

        @Override
        public boolean markWelcomeNotified(Long id) {
            return false;
        }

        @Override
        public void withdraw(Long id) {
        }
    }
}
