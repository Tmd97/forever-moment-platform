package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.VendorRequestDto;
import com.forvmom.common.dto.response.VendorResponseDto;
import com.forvmom.common.enums.VendorStatus;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/vendors")
@Tag(name = "Admin Vendor API", description = "Endpoints for managing vendors (Admin only)")
public class VendorControllerAdmin {

    @Autowired
    private VendorService vendorService;

    @GetMapping
    @Operation(summary = "Search and Filter Vendors", description = "Returns a list of vendors matching search term, category, and status filter")
    public ResponseEntity<ApiResponse<?>> searchVendors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) VendorStatus status) {
        List<VendorResponseDto> response = vendorService.searchVendors(query, category, status);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PostMapping
    @Operation(summary = "Create/Register Vendor", description = "Admin registers a new vendor into the platform")
    public ResponseEntity<ApiResponse<?>> registerVendor(@Valid @RequestBody VendorRequestDto request) {
        VendorResponseDto response = vendorService.registerVendorByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Vendor Details", description = "Retrieves vendor details by vendor ID")
    public ResponseEntity<ApiResponse<?>> getVendorById(@PathVariable Long id) {
        VendorResponseDto response = vendorService.getVendorById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Vendor", description = "Admin updates a vendor's business details or status (ACTIVE, PENDING, INACTIVE)")
    public ResponseEntity<ApiResponse<?>> updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody VendorRequestDto request) {
        VendorResponseDto response = vendorService.updateVendorByAdmin(id, request);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete / Deregister Vendor", description = "Admin deregisters/deletes a vendor account")
    public ResponseEntity<ApiResponse<?>> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendorByAdmin(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}