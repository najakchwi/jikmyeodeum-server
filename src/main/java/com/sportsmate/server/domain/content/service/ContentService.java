package com.sportsmate.server.domain.content.service;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.content.port.in.ContentUseCase;
import com.sportsmate.server.domain.content.port.out.ContentOutPort;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.ProfileOption;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentService implements ContentUseCase {

    private final ContentOutPort contentOutPort;
    private final TeamOutPort teamOutPort;
    private final ObjectStorage objectStorage;

    public ContentService(ContentOutPort contentOutPort, TeamOutPort teamOutPort, ObjectStorage objectStorage) {
        this.contentOutPort = contentOutPort;
        this.teamOutPort = teamOutPort;
        this.objectStorage = objectStorage;
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
                profileOptions());
    }

    private ProfileOptions profileOptions() {
        return new ProfileOptions(
                options(WatchStyle.values()),
                options(Personality.values()),
                options(TalkStyle.values()),
                options(SmokingStatus.values()),
                options(GenderPref.values()),
                options(AgePref.values()),
                options(SmokingPref.values()));
    }

    private List<EnumOption> options(ProfileOption[] options) {
        return Arrays.stream(options)
                .map(option -> new EnumOption(option.value(), option.label(), option.description()))
                .toList();
    }

    private String urlOrNull(String objectKey) {
        return objectKey == null || objectKey.isBlank() ? null : objectStorage.getUrl(objectKey);
    }
}
