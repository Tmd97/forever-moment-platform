package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.request.VendorRequestDto;
import com.forvmom.common.dto.response.VendorResponseDto;
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

/**
 * Public and Vendor self-service endpoints.
 *
 * <p>
 * Vendors can self-register (`/public/vendors/register`).
 * Once registered, vendors manage only their own profile (`/vendor/profile`).
 * Vendors CANNOT view or access other vendors' profiles.
 */
@RestController
@Tag(name = "Vendor API", description = "Endpoints for Vendor registration and self-service profile management")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    /**
     * Public self-registration for new vendors.
     *
     * @param request registration request
     * @return {@code 201 Created} with registered vendor profile
     */
    @PostMapping("/public/vendors/register")
    @Operation(summary = "Vendor Self Registration", description = "Allows a new vendor to register on the platform. Initial status will be PENDING approval.")
    public ResponseEntity<ApiResponse<?>> selfRegisterVendor(@RequestBody VendorRequestDto request) {
        VendorResponseDto response = vendorService.selfRegisterVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Fetches the profile of the currently authenticated vendor.
     *
     * @return {@code 200 OK} with logged-in vendor profile
     */
    @GetMapping("/vendor/profile")
    @Operation(summary = "Get Current Vendor Profile", description = "Returns the profile of the currently authenticated vendor. Vendors cannot see other vendors.")
    public ResponseEntity<ApiResponse<?>> getCurrentVendorProfile() {
        VendorResponseDto response = vendorService.getCurrentVendorProfile();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Creates or updates the profile of the currently authenticated vendor.
     * Allows vendor to populate business details upon first login or update status (e.g. INACTIVE).
     *
     * @param request profile fields (businessName, category, contactName, contactPhone, status)
     * @return {@code 200 OK} with updated vendor profile
     */
    @PostMapping("/vendor/profile")
    @Operation(summary = "Create or Update Current Vendor Profile", description = "Creates or updates the profile/status of the currently authenticated vendor.")
    public ResponseEntity<ApiResponse<?>> createOrUpdateProfilePost(@RequestBody VendorRequestDto request) {
        VendorResponseDto response = vendorService.createOrUpdateCurrentVendorProfile(request);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Updates the profile or status of the currently authenticated vendor.
     *
     * @param request profile fields
     * @return {@code 200 OK} with updated vendor profile
     */
    @PutMapping("/vendor/profile")
    @Operation(summary = "Update Current Vendor Profile", description = "Updates the profile or status (ACTIVE/INACTIVE) of the currently authenticated vendor.")
    public ResponseEntity<ApiResponse<?>> updateCurrentVendorProfile(@RequestBody VendorRequestDto request) {
        VendorResponseDto response = vendorService.createOrUpdateCurrentVendorProfile(request);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Deregisters the currently authenticated vendor account.
     *
     * @return {@code 200 OK} confirmation message
     */
    @DeleteMapping("/vendor/profile")
    @Operation(summary = "Deregister Current Vendor", description = "Allows the currently authenticated vendor to self-deregister their account.")
    public ResponseEntity<ApiResponse<?>> deregisterCurrentVendor() {
        vendorService.deregisterCurrentVendor();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}