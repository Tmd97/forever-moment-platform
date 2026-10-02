package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.FaqRequestDto;
import com.forvmom.common.dto.response.FaqResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for managing the global FAQ list shown on the public Help
 * page.
 *
 * <p>
 * FAQs are a flat, admin-ordered list — there are no categories and they are
 * not attached to individual experiences.
 */
@RestController
@RequestMapping("/admin/faqs")
@Tag(name = "Admin FAQ API", description = "Endpoints for managing FAQs (Admin only)")
public class FaqControllerAdmin {

    @Autowired
    private FaqService faqService;

    @PostMapping
    @Operation(summary = "Create FAQ", description = "Create a new FAQ entry")
    public ResponseEntity<ApiResponse<?>> createFaq(@Valid @RequestBody FaqRequestDto requestDto) {
        FaqResponseDto response = faqService.createFaq(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    @GetMapping
    @Operation(summary = "Get All FAQs", description = "Fetch all FAQs, including inactive ones")
    public ResponseEntity<ApiResponse<?>> getAllFaqs() {
        List<FaqResponseDto> response = faqService.getAllFaqsAdmin();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update FAQ", description = "Update an existing FAQ")
    public ResponseEntity<ApiResponse<?>> updateFaq(@PathVariable Long id,
            @Valid @RequestBody FaqRequestDto requestDto) {
        FaqResponseDto response = faqService.updateFaq(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete FAQ", description = "Soft delete an FAQ")
    public ResponseEntity<ApiResponse<?>> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle FAQ Active", description = "Toggle is_active for an FAQ")
    public ResponseEntity<ApiResponse<?>> toggleFaq(@PathVariable Long id) {
        faqService.toggleActive(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "FAQ status toggled successfully"));
    }

    /**
     * Resequences FAQ display order to match the id order supplied in the body,
     * as produced by a drag-and-drop reorder in the admin UI.
     *
     * @param orderedIds every FAQ id, in its new display order
     * @return {@code 200 OK} with an empty payload
     */
    @PutMapping("/reorder")
    @Operation(summary = "Reorder FAQs", description = "Resequence FAQ display order using the given id order")
    public ResponseEntity<ApiResponse<?>> reorderFaqs(@RequestBody List<Long> orderedIds) {
        faqService.reorderFaqs(orderedIds);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "FAQs reordered successfully"));
    }
}
