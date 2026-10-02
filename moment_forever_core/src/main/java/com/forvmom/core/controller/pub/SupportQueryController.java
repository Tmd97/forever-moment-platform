package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.request.SupportQueryRequestDto;
import com.forvmom.common.dto.response.SupportQueryResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.SupportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for the contact/support form.
 *
 * <p>
 * Mapped under {@code /public/support}, so it is reachable by both guests and
 * logged-in users. Caller identity, when present, is supplied by the API
 * gateway as the {@code X-User-Id} header rather than read from the security
 * context (see {@code BookingControllerAdmin} for the same pattern). When the
 * header is absent the submission is treated as a guest query and
 * {@code name}/{@code email}/{@code phone} must be supplied in the body.
 */
@RestController
@RequestMapping("/public/support")
@Tag(name = "Public Support API", description = "Endpoints for submitting a contact/support query")
public class SupportQueryController {

    @Autowired
    private SupportQueryService supportQueryService;

    @PostMapping
    @Operation(summary = "Submit Support Query", description = "Submit a contact/support query as a guest or a logged-in user")
    public ResponseEntity<ApiResponse<?>> submitQuery(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody SupportQueryRequestDto requestDto) {
        SupportQueryResponseDto response = supportQueryService.submitQuery(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }
}
