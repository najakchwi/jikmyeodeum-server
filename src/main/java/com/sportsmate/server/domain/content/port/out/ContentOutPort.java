package com.sportsmate.server.domain.content.port.out;

import com.sportsmate.server.domain.content.AvatarPreset;
import com.sportsmate.server.domain.content.BannerContent;
import com.sportsmate.server.domain.content.ContentAsset;
import com.sportsmate.server.domain.content.FaqContent;
import java.time.LocalDateTime;
import java.util.List;

public interface ContentOutPort {

    List<BannerContent> findActiveBanners(LocalDateTime now);

    List<FaqContent> findActiveFaqs();

    List<AvatarPreset> findActiveAvatarPresets();

    List<ContentAsset> findActiveAssets();
}
