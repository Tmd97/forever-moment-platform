package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.PromotionAssetRequestDto;
import com.forvmom.common.dto.response.PromotionImageResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.core.services.PromotionAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/promotions/assets")
@Tag(name = "Admin Promotion Asset API", description = "Manage scheduled promotion banners bound to global media")
public class PromotionAssetControllerAdmin {

    @Autowired
    private PromotionAssetService promotionAssetService;

    @PostMapping
    @Operation(summary = "Create promotion asset")
    public ResponseEntity<ApiResponse<PromotionImageResponseDto>> create(
            @Valid @RequestBody PromotionAssetRequestDto requestDto) {
        PromotionImageResponseDto response = promotionAssetService.create(requestDto);
        return ResponseEntity.ok(ResponseUtil.buildCreatedResponse(response, "Promotion asset created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotion asset")
    public ResponseEntity<ApiResponse<PromotionImageResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody PromotionAssetRequestDto requestDto) {
        PromotionImageResponseDto response = promotionAssetService.update(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, "Promotion asset updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotion asset")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promotionAssetService.delete(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "Promotion asset deleted"));
    }

    @GetMapping
    @Operation(summary = "List promotion assets")
    public ResponseEntity<ApiResponse<List<PromotionImageResponseDto>>> list(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String placement,
            @RequestParam(required = false) Boolean isActive) {
        List<PromotionImageResponseDto> response = promotionAssetService.listForAdmin(key, placement, isActive);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, "Promotion assets fetched"));
    }
}
