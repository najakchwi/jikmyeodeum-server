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
            ProfileOptions profileOptions
    ) {}

    record Banner(String code, String title, String imageUrl, String linkUrl) {}

    record Faq(String code, String category, String question, String answer) {}

    record Avatar(String code, String name, String imageUrl) {}

    record Asset(String type, String code, String name, String url) {}

    record Team(Long id, String name, String shortName, String emblemUrl, String primaryColorHex) {}

    record ProfileOptions(
            List<EnumOption> watchStyles,
            List<EnumOption> personalities,
            List<EnumOption> talkStyles,
            List<EnumOption> smokingStatuses,
            List<EnumOption> genderPrefs,
            List<EnumOption> agePrefs,
            List<EnumOption> smokingPrefs
    ) {}

    record EnumOption(String value, String label, String description) {}
}
