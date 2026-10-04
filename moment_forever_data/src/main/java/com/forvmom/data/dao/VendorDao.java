package com.forvmom.data.dao;

import com.forvmom.common.enums.VendorStatus;
import com.forvmom.data.entities.Vendor;

import java.util.List;
import java.util.Optional;

public interface VendorDao extends GenericDao<Vendor, Long> {
    Optional<Vendor> findByAuthUserId(Long authUserId);
    Optional<Vendor> findByContactEmailIgnoreCase(String email);
    Optional<Vendor> findByVendorCode(String vendorCode);
    boolean existsByContactEmailIgnoreCase(String email);
    boolean existsByVendorCode(String vendorCode);
    List<Vendor> searchVendors(String searchTerm, String category, VendorStatus status);
}