package com.forvmom.core.services;

import com.forvmom.common.dto.request.PromotionAssetRequestDto;
import com.forvmom.common.dto.response.PromotionImageResponseDto;

import java.util.List;

public interface PromotionAssetService {

    PromotionImageResponseDto create(PromotionAssetRequestDto requestDto);

    PromotionImageResponseDto update(Long id, PromotionAssetRequestDto requestDto);

    void delete(Long id);

    List<PromotionImageResponseDto> listForAdmin(String key, String placement, Boolean isActive);

    List<PromotionImageResponseDto> listForPublic(String key, String placement);

    PromotionImageResponseDto getSingleForPublic(String key, String placement);
}
