package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.response.FaqResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoint serving the FAQ accordion on the storefront.
 */
@RestController
@RequestMapping("/public/faqs")
@Tag(name = "Public FAQ API", description = "Endpoints for the public FAQ page")
public class FaqController {

    @Autowired
    private FaqService faqService;

    @GetMapping
    @Operation(summary = "Get Active FAQs", description = "Fetch active FAQs ordered for display")
    public ResponseEntity<ApiResponse<?>> getActiveFaqs() {
        List<FaqResponseDto> response = faqService.getActiveFaqs();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
