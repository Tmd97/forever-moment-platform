package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.response.PromotionImageResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.core.services.PromotionAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/public/promotions/assets")
@Tag(name = "Public Promotion Asset API", description = "Fetch active scheduled promotion assets by key and optional placement")
public class PromotionAssetController {

    @Autowired
    private PromotionAssetService promotionAssetService;

    @GetMapping
    @Operation(summary = "Get promotion asset(s) by key and optional placement")
    public ResponseEntity<ApiResponse<?>> getPromotions(
            @RequestParam("key") String key,
            @RequestParam(value = "placement", required = false) String placement,
            @RequestParam(value = "single", defaultValue = "false") boolean single) {

        if (single) {
            PromotionImageResponseDto image = promotionAssetService.getSingleForPublic(key, placement);
            if (image == null) {
                return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "No active promotion asset found"));
            }
            return ResponseEntity.ok(ResponseUtil.buildOkResponse(image, "Promotion asset fetched"));
        }

        List<PromotionImageResponseDto> images = promotionAssetService.listForPublic(key, placement);
        if (images == null) {
            images = Collections.emptyList();
        }
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(images, "Promotion assets fetched"));
    }
}
