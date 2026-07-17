package com.sportsmate.server.domain.content.service;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.content.port.in.ContentUseCase;
import com.sportsmate.server.domain.content.port.out.ContentOutPort;
import com.sportsmate.server.domain.game.port.out.LeagueOutPort;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import com.sportsmate.server.domain.content.port.in.ContentUseCase.ProfileOptionGroup;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.DrinkingPref;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.FanLevelPref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.MeetPurpose;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.ProfileOption;
import com.sportsmate.server.domain.member.enums.SeatZone;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkPref;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.TeamPref;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional(readOnly = true)
public class ContentService implements ContentUseCase {

    private final ContentOutPort contentOutPort;
    private final LeagueOutPort leagueOutPort;
    private final TeamOutPort teamOutPort;
    private final ObjectStorage objectStorage;

    @Autowired
    public ContentService(ContentOutPort contentOutPort, LeagueOutPort leagueOutPort,
            TeamOutPort teamOutPort, ObjectStorage objectStorage) {
        this.contentOutPort = contentOutPort;
        this.leagueOutPort = leagueOutPort;
        this.teamOutPort = teamOutPort;
        this.objectStorage = objectStorage;
    }

    public ContentService(ContentOutPort contentOutPort, TeamOutPort teamOutPort, ObjectStorage objectStorage) {
        this(contentOutPort, () -> List.of(), teamOutPort, objectStorage);
    }

    @Override
    public BootstrapContent getBootstrapContent() {
        LocalDateTime now = LocalDateTime.now();
        return new BootstrapContent(
                contentOutPort.findActiveBanners(now).stream()
                        .map(banner -> new Banner(
                                banner.code(),
                                banner.title(),
                                urlOrNull(banner.imageKey()),
                                banner.linkUrl()))
                        .toList(),
                contentOutPort.findActiveFaqs().stream()
                        .map(faq -> new Faq(faq.code(), faq.category(), faq.question(), faq.answer()))
                        .toList(),
                contentOutPort.findActiveAvatarPresets().stream()
                        .map(avatar -> new Avatar(avatar.code(), avatar.name(), urlOrNull(avatar.imageKey())))
                        .toList(),
                contentOutPort.findActiveAssets().stream()
                        .map(asset -> new Asset(asset.type(), asset.code(), asset.name(), urlOrNull(asset.objectKey())))
                        .toList(),
                teamOutPort.findAll().stream()
                        .map(team -> new Team(
                                team.id(),
                                team.name(),
                                team.shortName(),
                                urlOrNull(team.emblemImageKey()),
                                team.primaryColorHex()))
                        .toList(),
                leagues(),
                profileOptions());
    }

    private List<League> leagues() {
        return leagueOutPort.findAll().stream()
                .map(league -> new League(
                        league.id(),
                        league.sport(),
                        league.code(),
                        league.name(),
                        teamOutPort.findByLeagueId(league.id()).stream()
                                .map(team -> new Team(
                                        team.id(),
                                        team.name(),
                                        team.shortName(),
                                        urlOrNull(team.emblemImageKey()),
                                        team.primaryColorHex()))
                                .toList()))
                .toList();
    }

    private ProfileOptions profileOptions() {
        return new ProfileOptions(
                new ProfileOptionGroup(
                        options(WatchStyle.activeValues()),
                        WatchStyle.MULTI_SELECTABLE,
                        WatchStyle.MAX_COUNT),
                new ProfileOptionGroup(
                        options(SeatZone.activeValues()),
                        SeatZone.MULTI_SELECTABLE,
                        SeatZone.MAX_COUNT),
                new ProfileOptionGroup(
                        options(FanLevel.activeValues()),
                        FanLevel.MULTI_SELECTABLE,
                        FanLevel.MAX_COUNT),
                new ProfileOptionGroup(
                        options(Personality.activeValues()),
                        Personality.MULTI_SELECTABLE,
                        Personality.MAX_COUNT),
                new ProfileOptionGroup(
                        options(TalkStyle.activeValues()),
                        TalkStyle.MULTI_SELECTABLE,
                        TalkStyle.MAX_COUNT),
                new ProfileOptionGroup(
                        options(SmokingStatus.activeValues()),
                        SmokingStatus.MULTI_SELECTABLE,
                        SmokingStatus.MAX_COUNT),
                new ProfileOptionGroup(
                        options(DrinkingStatus.activeValues()),
                        DrinkingStatus.MULTI_SELECTABLE,
                        DrinkingStatus.MAX_COUNT),
                new ProfileOptionGroup(
                        options(MeetPurpose.activeValues()),
                        MeetPurpose.MULTI_SELECTABLE,
                        MeetPurpose.MAX_COUNT),
                options(GenderPref.values()),
                options(AgePref.values()),
                options(SmokingPref.values()),
                options(DrinkingPref.values()),
                options(FanLevelPref.values()),
                options(TalkPref.values()),
                options(TeamPref.values()));
    }

    private List<EnumOption> options(List<? extends ProfileOption> options) {
        return options.stream()
                .map(option -> new EnumOption(option.value(), option.label(), option.description()))
                .toList();
    }

    private List<EnumOption> options(ProfileOption[] options) {
        return Arrays.stream(options)
                .filter(ProfileOption::active)
                .map(option -> new EnumOption(option.value(), option.label(), option.description()))
                .toList();
    }

    private String urlOrNull(String objectKey) {
        return objectKey == null || objectKey.isBlank() ? null : objectStorage.getUrl(objectKey);
    }
}
