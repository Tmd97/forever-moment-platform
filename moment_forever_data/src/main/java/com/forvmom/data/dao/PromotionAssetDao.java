package com.forvmom.data.dao;

import com.forvmom.data.entities.PromotionAsset;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionAssetDao extends GenericDao<PromotionAsset, Long> {

    List<PromotionAsset> findByFilters(String promoKey, String placement, Boolean isActive);

    List<PromotionAsset> findActiveByKeyAndPlacement(String promoKey, String placement, LocalDateTime now);

    boolean existsByMediaId(Long mediaId);
}
