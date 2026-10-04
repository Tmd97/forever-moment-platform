package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.VendorResponseDto;
import com.forvmom.data.entities.Vendor;

public class VendorBeanMapper {

    public static VendorResponseDto mapEntityToDto(Vendor vendor) {
        if (vendor == null) {
            return null;
        }
        VendorResponseDto dto = new VendorResponseDto();
        dto.setId(vendor.getId());
        dto.setVendorCode(vendor.getVendorCode());
        dto.setBusinessName(vendor.getBusinessName());
        dto.setCategory(vendor.getCategory());
        dto.setContactName(vendor.getContactName());
        dto.setContactEmail(vendor.getContactEmail());
        dto.setContactPhone(vendor.getContactPhone());
        dto.setStatus(vendor.getStatus());
        if (vendor.getAuthUser() != null) {
            dto.setAuthUserId(vendor.getAuthUser().getId());
        }
        dto.setCreatedOn(vendor.getCreatedOn());
        dto.setUpdatedOn(vendor.getUpdatedOn());
        return dto;
    }
}