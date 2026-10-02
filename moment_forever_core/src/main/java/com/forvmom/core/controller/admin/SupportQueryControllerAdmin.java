package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.response.SupportQueryResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.SupportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for reviewing and resolving support queries submitted
 * through the public contact form.
 */
@RestController
@RequestMapping("/admin/support")
@Tag(name = "Admin Support API", description = "Endpoints for managing support queries (Admin only)")
public class SupportQueryControllerAdmin {

    @Autowired
    private SupportQueryService supportQueryService;

    @GetMapping
    @Operation(summary = "Get All Support Queries", description = "Fetch all support queries, optionally filtered by status (OPEN/RESOLVED)")
    public ResponseEntity<ApiResponse<?>> getAllQueries(
            @RequestParam(required = false) String status) {
        List<SupportQueryResponseDto> response = supportQueryService.getAllQueriesAdmin(status);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Support Query by ID", description = "Fetch a single support query")
    public ResponseEntity<ApiResponse<?>> getQueryById(@PathVariable Long id) {
        SupportQueryResponseDto response = supportQueryService.getQueryById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Mark Support Query Resolved", description = "Mark a support query as resolved")
    public ResponseEntity<ApiResponse<?>> markResolved(@PathVariable Long id) {
        SupportQueryResponseDto response = supportQueryService.markResolved(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }
}
