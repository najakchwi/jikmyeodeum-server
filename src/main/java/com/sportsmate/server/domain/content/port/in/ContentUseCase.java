package com.sportsmate.server.domain.content.port.in;

import java.util.List;

public interface ContentUseCase {

    BootstrapContent getBootstrapContent();

    record BootstrapContent(
            List<Banner> banners,
            List<Faq> faqs,
            List<Avatar> avatars,
            List<Asset> assets,
            List<Team> teams,
            List<League> leagues,
            ProfileOptions profileOptions
    ) {}

    record Banner(String code, String title, String imageUrl, String linkUrl) {}

    record Faq(String code, String category, String question, String answer) {}

    record Avatar(String code, String name, String imageUrl) {}

    record Asset(String type, String code, String name, String url) {}

    record Team(Long id, String name, String shortName, String emblemUrl, String primaryColorHex) {}

    record League(Long id, String sport, String code, String name, List<Team> teams) {}

    record ProfileOptions(
            ProfileOptionGroup watchStyles,
            ProfileOptionGroup seatZones,
            ProfileOptionGroup fanLevels,
            ProfileOptionGroup personalities,
            ProfileOptionGroup talkStyles,
            ProfileOptionGroup smokingStatuses,
            ProfileOptionGroup drinkingStatuses,
            ProfileOptionGroup meetPurposes,
            List<EnumOption> genderPrefs,
            List<EnumOption> agePrefs,
            List<EnumOption> smokingPrefs,
            List<EnumOption> drinkingPrefs,
            List<EnumOption> fanLevelPrefs,
            List<EnumOption> talkPrefs,
            List<EnumOption> teamPrefs
    ) {}

    record ProfileOptionGroup(List<EnumOption> options, boolean multiSelectable, Integer maxCount) {}

    record EnumOption(String value, String label, String description) {}
}
