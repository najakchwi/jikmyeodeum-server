package com.sportsmate.server.domain.member.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.location.KakaoLocalApiPort;
import com.sportsmate.server.common.port.out.location.LocationRegion;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {

    private final MemberOutPort memberOutPort;
    private final KakaoLocalApiPort kakaoLocalApi;

    public MemberService(MemberOutPort memberOutPort, KakaoLocalApiPort kakaoLocalApi) {
        this.memberOutPort = memberOutPort;
        this.kakaoLocalApi = kakaoLocalApi;
    }

    @Override
    public MemberProfile get(Long memberId) {
        return MemberProfile.from(find(memberId));
    }

    @Override
    @Transactional
    public MemberProfile updateProfile(Long memberId, String nickname, String bio,
            LocalDate birthdate, Gender gender, String team) {
        Member member = find(memberId);
        if (nickname != null && !nickname.equals(member.getNickname())
                && memberOutPort.existsByNickname(nickname)) {
            throw new BusinessException(MemberErrorCode.NICKNAME_ALREADY_USED);
        }
        member.updateProfile(nickname, bio, birthdate, gender, team);
        return MemberProfile.from(memberOutPort.save(member));
    }

    @Override
    @Transactional
    public MemberProfile updateStyle(Long memberId, String team, List<WatchStyle> watchStyles,
            Personality personality, TalkStyle talkStyle, SmokingStatus smokingStatus) {
        if (watchStyles != null && watchStyles.size() > 2) {
            throw new BusinessException(com.sportsmate.server.common.exception.CommonErrorCode.INVALID_INPUT);
        }
        Member member = find(memberId);
        member.updateStyle(team, watchStyles, personality, talkStyle, smokingStatus);
        return MemberProfile.from(memberOutPort.save(member));
    }

    @Override
    @Transactional
    public MemberProfile updatePreference(Long memberId, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, Integer distanceKm) {
        Member member = find(memberId);
        member.updatePreference(genderPref, agePref, smokingPref, distanceKm);
        return MemberProfile.from(memberOutPort.save(member));
    }

    @Override
    @Transactional
    public LocationVerifyResult verifyLocation(Long memberId, double latitude, double longitude) {
        LocationRegion region = kakaoLocalApi.reverseGeocode(latitude, longitude);
        Member member = find(memberId);
        member.verifyLocation(region.toAddress(), latitude, longitude);
        memberOutPort.save(member);
        return new LocationVerifyResult(
                true,
                region.depth1(),
                region.depth2(),
                region.depth3());
    }

    @Override
    public TrustScoreResult getTrustScore(Long memberId) {
        Member member = find(memberId);
        String grade = member.getTrustScore() >= 90 ? "우수" :
                member.getTrustScore() >= 70 ? "양호" :
                member.getTrustScore() >= 50 ? "주의" : "이용 제한";
        return new TrustScoreResult(
                member.getTrustScore(),
                grade,
                Math.max(1, 100 - member.getTrustScore()),
                List.of(
                        new TrustScoreResult.GradeRange("우수", 90, 100),
                        new TrustScoreResult.GradeRange("양호", 70, 89),
                        new TrustScoreResult.GradeRange("주의", 50, 69),
                        new TrustScoreResult.GradeRange("이용 제한", 0, 49)),
                List.of(
                        new TrustScoreResult.Rule("약속 미이행 / 불참", -10),
                        new TrustScoreResult.Rule("신고 접수", -15),
                        new TrustScoreResult.Rule("매칭 취소 (반복)", -5),
                        new TrustScoreResult.Rule("비매너 행동", -20),
                        new TrustScoreResult.Rule("프로필 허위 기재", -10),
                        new TrustScoreResult.Rule("채팅 미응답 (24h)", -3),
                        new TrustScoreResult.Rule("상대방으로부터 2점 평가 받기", -3),
                        new TrustScoreResult.Rule("상대방으로부터 1점 평가 받기", -5)),
                List.of(
                        new TrustScoreResult.Rule("경기 후 평가 참여", 3),
                        new TrustScoreResult.Rule("상대방으로부터 5점 평가 받기", 5),
                        new TrustScoreResult.Rule("상대방으로부터 4점 평가 받기", 2),
                        new TrustScoreResult.Rule("7일 연속 앱 이용", 2),
                        new TrustScoreResult.Rule("신고 없이 매칭 완료", 3)));
    }

    @Override
    @Transactional
    public MemberProfile updateAvatar(Long memberId, String avatarUrl) {
        Member member = find(memberId);
        member.changeAvatar(avatarUrl);
        return MemberProfile.from(memberOutPort.save(member));
    }

    private Member find(Long memberId) {
        return memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
