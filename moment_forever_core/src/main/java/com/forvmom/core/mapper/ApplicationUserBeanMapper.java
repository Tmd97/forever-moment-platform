package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.AdminAppUserResponseDto;
import com.forvmom.common.dto.response.AppUserResponseDto;
import com.forvmom.common.dto.request.UserProfileRequestDto;
import com.forvmom.data.entities.ApplicationUser;

import java.util.List;

/**
 * Static conversions between {@link ApplicationUser} entities and the user
 * profile DTOs.
 *
 * <p>
 * Two read projections exist: the end-user view, which carries profile fields
 * only, and the admin view, which additionally flattens the linked
 * authentication account (credential flags, timestamps and role names/ids).
 */
public class ApplicationUserBeanMapper {

    /**
     * Copies the editable profile fields from a request DTO onto an existing
     * entity. Does nothing when either argument is {@code null}.
     *
     * @param dto    source of the new profile values
     * @param entity entity to mutate in place
     */
    public static void mapDtoToEntity(UserProfileRequestDto dto, ApplicationUser entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setFullName(dto.getFullName());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setProfilePictureUrl(dto.getProfilePictureUrl());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setPreferredCity(dto.getPreferredCity());
    }

    /**
     * Maps an entity to the end-user profile projection, which deliberately omits
     * all authentication data.
     *
     * @param entity the user entity, may be {@code null}
     * @return the profile DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static AppUserResponseDto mapEntityToDto(ApplicationUser entity) {
        if (entity == null) {
            return null;
        }
        AppUserResponseDto dto = new AppUserResponseDto();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setProfilePictureUrl(entity.getProfilePictureUrl());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setPreferredCity(entity.getPreferredCity());
        return dto;
    }

    /**
     * Maps an entity to the admin projection: profile fields plus the linked
     * authentication account, including its credential flags and the names and ids
     * of the assigned roles. Auth fields are left unset when no account is linked.
     *
     * @param entity the user entity, may be {@code null}
     * @return the admin DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static AdminAppUserResponseDto mapEntityToAdminDto(
            ApplicationUser entity) {
        if (entity == null) {
            return null;
        }
        AdminAppUserResponseDto dto = new AdminAppUserResponseDto();
        // Map basic fields
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setProfilePictureUrl(entity.getProfilePictureUrl());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setPreferredCity(entity.getPreferredCity());

        // Map Admin specific fields
        if (entity.getAuthUser() != null) {
            dto.setAuthUserId(entity.getAuthUser().getId());
            dto.setUsername(entity.getAuthUser().getUsername());
            dto.setEnabled(entity.getAuthUser().isEnabled());
            dto.setAccountNonLocked(entity.getAuthUser().isAccountNonLocked());
            dto.setAccountNonExpired(entity.getAuthUser().isAccountNonExpired());
            dto.setCredentialsNonExpired(entity.getAuthUser().isCredentialsNonExpired());
            dto.setExternalUserId(entity.getAuthUser().getExternalUserId());
            dto.setAuthCreatedAt(entity.getAuthUser().getCreatedAt());
            dto.setAuthLastLogin(entity.getAuthUser().getLastLogin());

            if (entity.getAuthUser().getUserRoles() != null) {
                List<String> roles = entity.getAuthUser().getUserRoles().stream()
                        .map(authUserRole -> authUserRole.getRole().getName())
                        .collect(java.util.stream.Collectors.toList());
                dto.setRoles(roles);
            }

            if (entity.getAuthUser().getUserRoles() != null) {
                List<Long> rolesIds = entity.getAuthUser().getUserRoles().stream()
                        .map(authUserRole -> authUserRole.getRole().getId())
                        .collect(java.util.stream.Collectors.toList());
                dto.setRoleIds(rolesIds);
            }
        }
        return dto;
    }
}