package com.forvmom.core.services;

import com.forvmom.common.dto.request.VendorRequestDto;
import com.forvmom.common.dto.response.VendorResponseDto;
import com.forvmom.common.enums.VendorStatus;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.VendorBeanMapper;
import com.forvmom.data.dao.VendorDao;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.Vendor;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.config.PasswordConfig;
import com.forvmom.security.dto.JwtUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VendorServiceImpl implements VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorServiceImpl.class);

    @Autowired
    private VendorDao vendorDao;

    @Autowired
    private AuthUserDao authUserDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PasswordConfig passwordEncoder;

    // ── Admin Operations ─────────────────────────────────────────────────

    @Override
    @Transactional
    public VendorResponseDto registerVendorByAdmin(VendorRequestDto dto) {
        VendorStatus status = dto.getStatus() != null ? dto.getStatus() : VendorStatus.ACTIVE;
        return createVendorAccount(dto, status);
    }

    private VendorResponseDto createVendorAccount(VendorRequestDto dto, VendorStatus status) {
        if (authUserDao.existsByUsername(dto.getContactEmail()) || vendorDao.existsByContactEmailIgnoreCase(dto.getContactEmail())) {
            throw new CustomAuthException("Email already in use: " + dto.getContactEmail());
        }

        Role vendorRole = roleDao.findByNameIgnoreCase("VENDOR")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'VENDOR' not initialized in system"));

        AuthUser authUser = new AuthUser();
        authUser.setUsername(dto.getContactEmail());

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : "VendorSecret123!";
        authUser.setPassword(passwordEncoder.passwordEncoder().encode(rawPassword));
        authUser.addRole(vendorRole);
        authUser.setAccountNonExpired(true);
        authUser.setAccountNonLocked(true);
        authUser.setCredentialsNonExpired(true);
        authUser.setEnabled(true);

        AuthUser savedAuthUser = authUserDao.save(authUser);

        Vendor vendor = new Vendor();
        vendor.setBusinessName(dto.getBusinessName() != null ? dto.getBusinessName() : "Pending Vendor Setup");
        vendor.setCategory(dto.getCategory() != null ? dto.getCategory() : "General");
        vendor.setContactName(dto.getContactName() != null ? dto.getContactName() : dto.getContactEmail());
        vendor.setContactEmail(dto.getContactEmail());
        vendor.setContactPhone(dto.getContactPhone());
        vendor.setStatus(status);
        vendor.setAuthUser(savedAuthUser);

        Vendor savedVendor = vendorDao.save(vendor);
        savedVendor.setVendorCode("FMA-" + savedVendor.getId());
        Vendor updatedVendor = vendorDao.update(savedVendor);

        logger.info("Admin created vendor account: {} ({}) with status: {}", updatedVendor.getBusinessName(), updatedVendor.getVendorCode(), status);
        return VendorBeanMapper.mapEntityToDto(updatedVendor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorResponseDto> searchVendors(String query, String category, VendorStatus status) {
        List<Vendor> vendors = vendorDao.searchVendors(query, category, status);
        return vendors.stream()
                .map(VendorBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponseDto getVendorById(Long id) {
        Vendor vendor = vendorDao.findById(id);
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found with id: " + id);
        }
        return VendorBeanMapper.mapEntityToDto(vendor);
    }

    @Override
    @Transactional
    public VendorResponseDto updateVendorByAdmin(Long id, VendorRequestDto dto) {
        Vendor vendor = vendorDao.findById(id);
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found with id: " + id);
        }

        if (dto.getBusinessName() != null) vendor.setBusinessName(dto.getBusinessName());
        if (dto.getCategory() != null) vendor.setCategory(dto.getCategory());
        if (dto.getContactName() != null) vendor.setContactName(dto.getContactName());
        if (dto.getContactPhone() != null) vendor.setContactPhone(dto.getContactPhone());

        if (dto.getStatus() != null) {
            vendor.setStatus(dto.getStatus());
        }

        if (dto.getContactEmail() != null && !vendor.getContactEmail().equalsIgnoreCase(dto.getContactEmail())) {
            if (authUserDao.existsByUsername(dto.getContactEmail())) {
                throw new CustomAuthException("Email already in use: " + dto.getContactEmail());
            }
            AuthUser authUser = vendor.getAuthUser();
            authUser.setUsername(dto.getContactEmail());
            authUserDao.save(authUser);
            vendor.setContactEmail(dto.getContactEmail());
        }

        Vendor updated = vendorDao.update(vendor);
        return VendorBeanMapper.mapEntityToDto(updated);
    }

    @Override
    @Transactional
    public void deleteVendorByAdmin(Long id) {
        Vendor vendor = vendorDao.findById(id);
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found with id: " + id);
        }
        AuthUser authUser = vendor.getAuthUser();
        if (authUser != null) {
            authUser.setEnabled(false);
            authUser.setAccountNonLocked(false);
            authUserDao.save(authUser);
        }
        vendorDao.delete(vendor);
    }

    // ── Vendor Self-Service ───────────────────────────────────────────────

    @Override
    @Transactional
    public VendorResponseDto selfRegisterVendor(VendorRequestDto dto) {
        return createVendorAccount(dto, VendorStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponseDto getCurrentVendorProfile() {
        Long authUserId = getAuthenticatedAuthUserId();
        Optional<Vendor> vendorOptional = vendorDao.findByAuthUserId(authUserId);
        if (vendorOptional.isEmpty()) {
            // Profile not populated yet
            return null;
        }
        return VendorBeanMapper.mapEntityToDto(vendorOptional.get());
    }

    @Override
    @Transactional
    public VendorResponseDto createOrUpdateCurrentVendorProfile(VendorRequestDto dto) {
        Long authUserId = getAuthenticatedAuthUserId();
        AuthUser authUser = authUserDao.findById(authUserId);
        if (authUser == null) {
            throw new CustomAuthException("Authenticated user record not found");
        }

        Optional<Vendor> vendorOptional = vendorDao.findByAuthUserId(authUserId);
        Vendor vendor;
        if (vendorOptional.isPresent()) {
            vendor = vendorOptional.get();
        } else {
            // First-time profile creation by Vendor
            vendor = new Vendor();
            vendor.setAuthUser(authUser);
            vendor.setContactEmail(authUser.getUsername());
            vendor.setStatus(VendorStatus.ACTIVE);
        }

        if (dto.getBusinessName() != null) vendor.setBusinessName(dto.getBusinessName());
        if (dto.getCategory() != null) vendor.setCategory(dto.getCategory());
        if (dto.getContactName() != null) vendor.setContactName(dto.getContactName());
        if (dto.getContactPhone() != null) vendor.setContactPhone(dto.getContactPhone());

        // Vendor can set himself ACTIVE or INACTIVE
        if (dto.getStatus() != null) {
            vendor.setStatus(dto.getStatus());
        }

        if (dto.getContactEmail() != null && !vendor.getContactEmail().equalsIgnoreCase(dto.getContactEmail())) {
            if (authUserDao.existsByUsername(dto.getContactEmail())) {
                throw new CustomAuthException("Email already in use: " + dto.getContactEmail());
            }
            authUser.setUsername(dto.getContactEmail());
            authUserDao.save(authUser);
            vendor.setContactEmail(dto.getContactEmail());
        }

        Vendor saved = vendorDao.save(vendor);
        if (saved.getVendorCode() == null || saved.getVendorCode().isBlank()) {
            saved.setVendorCode("FMA-" + saved.getId());
            saved = vendorDao.update(saved);
        }

        logger.info("Vendor {} updated own profile/status ({})", saved.getBusinessName(), saved.getStatus());
        return VendorBeanMapper.mapEntityToDto(saved);
    }

    @Override
    @Transactional
    public void deregisterCurrentVendor() {
        Long authUserId = getAuthenticatedAuthUserId();
        Vendor vendor = vendorDao.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for authenticated user"));

        AuthUser authUser = vendor.getAuthUser();
        if (authUser != null) {
            authUser.setEnabled(false);
            authUser.setAccountNonLocked(false);
            authUserDao.save(authUser);
        }
        vendorDao.delete(vendor);
        logger.info("Vendor {} ({}) self-deregistered", vendor.getBusinessName(), vendor.getVendorCode());
    }

    private Long getAuthenticatedAuthUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomAuthException("No authenticated user");
        }
        Object o = authentication.getPrincipal();
        Long authUserId = extractAuthUserId(o);
        if (authUserId == null) {
            throw new CustomAuthException("Invalid principal type");
        }
        return authUserId;
    }

    private Long extractAuthUserId(Object o) {
        if (o instanceof JwtUserDetails) {
            return ((JwtUserDetails) o).getId();
        } else if (o instanceof UserDetails) {
            try {
                return Long.parseLong(((UserDetails) o).getUsername());
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (o instanceof String) {
            try {
                return Long.parseLong((String) o);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (o instanceof Long) {
            return (Long) o;
        }
        return null;
    }
}