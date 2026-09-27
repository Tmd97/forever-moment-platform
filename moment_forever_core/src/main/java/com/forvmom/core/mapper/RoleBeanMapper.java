package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.RoleRequestDto;
import com.forvmom.common.dto.response.RoleResponseDto;
import com.forvmom.data.entities.auth.Role;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static conversions between {@link Role} entities and their request and
 * response DTOs.
 *
 * <p>
 * Create and update behave differently on purpose: creation applies defaults
 * for the optional flags, whereas update only overwrites fields the caller
 * actually supplied and never changes the system-role flag.
 */
public class RoleBeanMapper {

    /**
     * Builds a new role entity from a create request, defaulting permission level
     * to {@code 10}, active to {@code true} and system role to {@code false} when
     * they are not supplied.
     *
     * @param dto the role definition, may be {@code null}
     * @return the new entity, or {@code null} if {@code dto} is {@code null}
     */
    public static Role mapDtoToEntity(RoleRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Role role = new Role();
        role.setName(dto.getRoleName());
        role.setDescription(dto.getDescription());

        // Set permission level with default value 10 if not provided
        role.setPermissionLevel(dto.getPermissionLevel() != null ? dto.getPermissionLevel() : 10);

        // Set active status with default true if not provided
        role.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Set system role flag with default false if not provided
        role.setSystemRole(dto.getSystemRole() != null ? dto.getSystemRole() : false);

        return role;
    }

    /**
     * Applies an update request to an existing role. Name and description are always
     * overwritten, while permission level and the active flag are only changed when
     * present in the request. The system-role flag is intentionally never updated,
     * as it is fixed at creation time.
     *
     * @param entity the role to mutate in place
     * @param dto    source of the new values
     */
    public static void updateEntity(Role entity, RoleRequestDto dto) {
        if (dto == null || entity == null) {
            return;
        }

        // Update allowed fields
        entity.setName(dto.getRoleName());
        entity.setDescription(dto.getDescription());

        // Update permission level if provided, otherwise keep existing
        if (dto.getPermissionLevel() != null) {
            entity.setPermissionLevel(dto.getPermissionLevel());
        }

        // Update active status if provided, otherwise keep existing
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }

        // NOTE: isSystemRole is NOT updated here intentionally
        // System role flag should only be set at creation time
    }

    /**
     * Maps a role entity to its response DTO. The user count is left {@code null}
     * because the {@link Role} entity holds no relationship to its users.
     *
     * @param entity the role entity, may be {@code null}
     * @return the DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static RoleResponseDto mapEntityToDto(Role entity) {
        if (entity == null) {
            return null;
        }

        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(entity.getId());
        dto.setRoleName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPermissionLevel(entity.getPermissionLevel());
        dto.setActive(entity.isActive());
        dto.setSystemRole(entity.isSystemRole());
        dto.setCreatedAt(entity.getCreatedAt());

        // User count is not set here - no relationship in Role entity
        // Set to null or 0 as per your response DTO
        dto.setUserCount(null); // Or dto.setUserCount(0);

        return dto;
    }

    /**
     * Maps a list of role entities to their response DTOs.
     *
     * @param entities the entities to convert, may be {@code null} or empty
     * @return the mapped DTOs, or an empty list when there is nothing to map
     */
    public static List<RoleResponseDto> toDtoList(List<Role> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(RoleBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Builds a role entity from a create request and then forces a specific
     * permission level, overriding whatever the request carried.
     *
     * @param dto             the role definition, may be {@code null}
     * @param permissionLevel permission level to apply; ignored when {@code null}
     * @return the new entity, or {@code null} if {@code dto} is {@code null}
     */
    public static Role mapDtoToEntityWithPermission(RoleRequestDto dto, Integer permissionLevel) {
        Role role = mapDtoToEntity(dto);
        if (role != null && permissionLevel != null) {
            role.setPermissionLevel(permissionLevel);
        }
        return role;
    }

    /**
     * Copies the descriptive and flag fields from one role to another, leaving the
     * target's identity ({@code id}) and creation timestamp untouched. Does nothing
     * when either argument is {@code null}.
     *
     * @param source role to read from
     * @param target role to write to
     */
    public static void copyProperties(Role source, Role target) {
        if (source == null || target == null) {
            return;
        }

        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setPermissionLevel(source.getPermissionLevel());
        target.setActive(source.isActive());
        target.setSystemRole(source.isSystemRole());
        // Note: id, createdAt are not copied
    }
}