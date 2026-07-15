package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.content.AvatarPreset;
import com.sportsmate.server.domain.content.BannerContent;
import com.sportsmate.server.domain.content.ContentAsset;
import com.sportsmate.server.domain.content.FaqContent;
import com.sportsmate.server.domain.content.port.out.ContentOutPort;
import java.time.LocalDateTime;
import java.util.List;

@PersistenceAdapter
public class ContentPersistenceAdapter implements ContentOutPort {

    private final BannerJpaRepository bannerRepository;
    private final FaqJpaRepository faqRepository;
    private final AvatarPresetJpaRepository avatarPresetRepository;
    private final ContentAssetJpaRepository contentAssetRepository;

    public ContentPersistenceAdapter(
            BannerJpaRepository bannerRepository,
            FaqJpaRepository faqRepository,
            AvatarPresetJpaRepository avatarPresetRepository,
            ContentAssetJpaRepository contentAssetRepository
    ) {
        this.bannerRepository = bannerRepository;
        this.faqRepository = faqRepository;
        this.avatarPresetRepository = avatarPresetRepository;
        this.contentAssetRepository = contentAssetRepository;
    }

    @Override
    public List<BannerContent> findActiveBanners(LocalDateTime now) {
        return bannerRepository.findActive(now).stream()
                .map(entity -> new BannerContent(
                        entity.getCode(),
                        entity.getTitle(),
                        entity.getImageKey(),
                        entity.getLinkUrl(),
                        entity.getDisplayOrder(),
                        entity.getStartsAt(),
                        entity.getEndsAt()))
                .toList();
    }

    @Override
    public List<FaqContent> findActiveFaqs() {
        return faqRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(entity -> new FaqContent(
                        entity.getCode(),
                        entity.getCategory(),
                        entity.getQuestion(),
                        entity.getAnswer(),
                        entity.getDisplayOrder()))
                .toList();
    }

    @Override
    public List<AvatarPreset> findActiveAvatarPresets() {
        return avatarPresetRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(entity -> new AvatarPreset(
                        entity.getCode(),
                        entity.getName(),
                        entity.getImageKey(),
                        entity.getDisplayOrder()))
                .toList();
    }

    @Override
    public List<ContentAsset> findActiveAssets() {
        return contentAssetRepository.findAllByActiveTrueOrderByTypeAscDisplayOrderAscIdAsc().stream()
                .map(entity -> new ContentAsset(
                        entity.getType(),
                        entity.getCode(),
                        entity.getName(),
                        entity.getObjectKey(),
                        entity.getDisplayOrder()))
                .toList();
    }
}
