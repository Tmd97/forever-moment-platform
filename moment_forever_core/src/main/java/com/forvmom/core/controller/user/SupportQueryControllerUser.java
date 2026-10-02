package com.forvmom.core.controller.user;

import com.forvmom.common.dto.response.SupportQueryResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.SupportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Logged-in user endpoint for viewing the caller's own support queries.
 *
 * <p>
 * Mapped under {@code /user/support}, so it requires any authenticated role.
 * Caller identity is supplied by the API gateway as the {@code X-User-Id}
 * header (see {@code BookingControllerAdmin} for the same pattern).
 */
@RestController
@RequestMapping("/user/support")
@Tag(name = "User Support API", description = "Endpoints for a logged-in user's own support queries")
public class SupportQueryControllerUser {

    @Autowired
    private SupportQueryService supportQueryService;

    @GetMapping
    @Operation(summary = "Get My Support Queries", description = "Fetch the logged-in user's own support queries")
    public ResponseEntity<ApiResponse<?>> getMyQueries(@RequestHeader("X-User-Id") Long userId) {
        List<SupportQueryResponseDto> response = supportQueryService.getMyQueries(userId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
