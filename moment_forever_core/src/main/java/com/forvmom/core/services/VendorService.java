package com.forvmom.core.services;

import com.forvmom.common.dto.request.VendorRequestDto;
import com.forvmom.common.dto.response.VendorResponseDto;
import com.forvmom.common.enums.VendorStatus;

import java.util.List;

public interface VendorService {
    // Admin operations
    VendorResponseDto registerVendorByAdmin(VendorRequestDto dto);
    List<VendorResponseDto> searchVendors(String query, String category, VendorStatus status);
    VendorResponseDto getVendorById(Long id);
    VendorResponseDto updateVendorByAdmin(Long id, VendorRequestDto dto);
    void deleteVendorByAdmin(Long id);

    // Vendor self-service (Strictly isolated to logged-in vendor)
    VendorResponseDto selfRegisterVendor(VendorRequestDto dto);
    VendorResponseDto getCurrentVendorProfile();
    VendorResponseDto createOrUpdateCurrentVendorProfile(VendorRequestDto dto);
    void deregisterCurrentVendor();
}