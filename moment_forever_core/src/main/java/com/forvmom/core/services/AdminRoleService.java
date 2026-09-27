package com.forvmom.core.services;

import com.forvmom.common.dto.request.AuthUserResponseDto;
import com.forvmom.common.dto.request.RoleRequestDto;
import com.forvmom.common.dto.response.RoleResponseDto;

import com.forvmom.common.errorhandler.NotAllowedCustomException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.ApplicationUserBeanMapper;
import com.forvmom.core.mapper.RoleBeanMapper;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.data.entities.auth.AuthUserRole;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.dto.AuthBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Administrative service for managing security {@link Role} definitions and for
 * inspecting which users hold a given role.
 *
 * <p>
 * Every public operation runs inside a Spring transaction: read operations are
 * marked {@code readOnly = true}, write operations use a read-write transaction.
 * Role names are treated case-insensitively for uniqueness checks, so two roles
 * that differ only by case cannot coexist.
 *
 * <p>
 * The {@code SUPER_ADMIN} role is protected: it cannot be updated, activated or
 * deactivated through this service. Note that {@link #deleteRole(Long)} performs
 * a hard delete (the soft-delete variant is present but commented out in the
 * source).
 */
@Service
public class AdminRoleService {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private AuthUserDao authUserDao;

    // TODO: (need discussion) System roles that cannot be deleted
    private static final List<String> SYSTEM_ROLES = List.of("SYSTEM", "SUPER_ADMIN");

    /**
     * Creates a new role from the supplied request.
     *
     * @param requestDto the role definition to persist; its name must not clash
     *                   (ignoring case) with an existing role
     * @return the persisted role as a response DTO
     * @throws IllegalArgumentException if a role with the same name already exists
     */
    @Transactional
    public RoleResponseDto createRole(RoleRequestDto requestDto) {
        //Only Super Admin can create system roles
        if (roleDao.existsByNameIgnoreCase(requestDto.getRoleName())) {
            throw new IllegalArgumentException(
                    "Role with name '" + requestDto.getRoleName() + "' already exists"
            );
        }
        Role role = RoleBeanMapper.mapDtoToEntity(requestDto);
        Role savedRole = roleDao.save(role);

        return RoleBeanMapper.mapEntityToDto(savedRole);
    }

    /**
     * Looks up a single role by its primary key.
     *
     * @param id the role identifier
     * @return the matching role as a response DTO
     * @throws ResourceNotFoundException if no role exists with the given id
     */
    @Transactional(readOnly = true)
    public RoleResponseDto getRoleById(Long id) {
        Role role = getRoleByIdValidation(id);
        return RoleBeanMapper.mapEntityToDto(role);
    }


    /**
     * Looks up a single role by name, ignoring case.
     *
     * @param name the role name to search for
     * @return the matching role as a response DTO
     * @throws ResourceNotFoundException if no role exists with the given name
     */
    @Transactional(readOnly = true)
    public RoleResponseDto getRoleByName(String name) {
        Optional<Role> role = roleDao.findByNameIgnoreCase(name);
        if (role.isEmpty()) {
            throw new ResourceNotFoundException("Role not found with name: " + name);
        }
        return RoleBeanMapper.mapEntityToDto(role.get());
    }

    /**
     * Returns every role currently flagged as active.
     *
     * @return the active roles, or an empty list when there are none
     */
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        List<Role> roles = roleDao.findByActiveTrue();
        return RoleBeanMapper.toDtoList(roles);
    }

    /**
     * Returns the active roles using the DAO's dedicated active-roles query.
     *
     * @return the active roles, or an empty list when there are none
     */
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllActiveRoles() {
        List<Role> roles = roleDao.getAllActiveRoles();
        return RoleBeanMapper.toDtoList(roles);
    }

    /**
     * Lists the authentication users that hold the given role, resolved through the
     * {@code AuthUserRole} join rows.
     *
     * @param roleId the role identifier to filter on
     * @return the users holding that role, or an empty list when there are none
     */
    @Transactional(readOnly = true)
    public List<AuthUserResponseDto> getAllUserByRole(Long roleId) {
        List<AuthUserRole> authUserRoles = authUserDao.findAuthUserByRole(roleId);
        List<AuthUser> authUserList = authUserRoles.stream()
                .map(AuthUserRole::getAuthUser)
                .toList();
        return authUserList.stream().map(AuthBeanMapper::mapEntityToDtoForAuth).collect(Collectors.toList());
    }

    /**
     * Returns the roles flagged as system roles.
     *
     * @return the system roles, or an empty list when there are none
     */
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getSystemRoles() {
        List<Role> roles = roleDao.findBySystemRoleTrue();
        return RoleBeanMapper.toDtoList(roles);
    }

    /**
     * Updates an existing role's attributes.
     *
     * @param id         the identifier of the role to update
     * @param requestDto the new role attributes
     * @return the updated role as a response DTO
     * @throws ResourceNotFoundException  if no role exists with the given id
     * @throws NotAllowedCustomException  if the target role is {@code SUPER_ADMIN}
     * @throws IllegalArgumentException   if the new name is already taken by
     *                                    another role
     */
    @Transactional
    public RoleResponseDto updateRole(Long id, RoleRequestDto requestDto) {
        Role role = getRoleByIdValidation(id);
        validIfSuperAdmin(role);
        // Check if name is being changed and already exists
        if (!role.getName().equalsIgnoreCase(requestDto.getRoleName()) &&
                roleDao.existsByNameIgnoreCase(requestDto.getRoleName())) {
            throw new IllegalArgumentException(
                    "Role with name '" + requestDto.getRoleName() + "' already exists"
            );
        }

        RoleBeanMapper.updateEntity(role, requestDto);
        Role updatedRole = roleDao.save(role);

        return RoleBeanMapper.mapEntityToDto(updatedRole);
    }

    /**
     * Permanently removes a role. This is a hard delete; the deactivate-instead
     * alternative is available through {@link #deactivateRole(Long)}.
     *
     * @param id the identifier of the role to delete
     * @throws ResourceNotFoundException if no role exists with the given id
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleByIdValidation(id);
        // Soft delete - just deactivate instead of actual delete
//        role.setActive(false);
//        roleDao.save(role);

        // Or hard delete if you prefer:
         roleDao.delete(role);
    }

    /**
     * Marks a role as active.
     *
     * @param id the identifier of the role to activate
     * @return the updated role as a response DTO
     * @throws ResourceNotFoundException if no role exists with the given id
     * @throws NotAllowedCustomException if the target role is {@code SUPER_ADMIN}
     */
    @Transactional
    public RoleResponseDto activateRole(Long id) {
        Role role = getRoleByIdValidation(id);
        validIfSuperAdmin(role);
        role.setActive(true);
        Role updatedRole = roleDao.save(role);
        return RoleBeanMapper.mapEntityToDto(updatedRole);
    }

    /**
     * Marks a role as inactive, which is the soft-delete equivalent for roles.
     *
     * @param id the identifier of the role to deactivate
     * @return the updated role as a response DTO
     * @throws ResourceNotFoundException if no role exists with the given id
     * @throws NotAllowedCustomException if the target role is {@code SUPER_ADMIN}
     */
    @Transactional
    public RoleResponseDto deactivateRole(Long id) {
        Role role = getRoleByIdValidation(id);
        validIfSuperAdmin(role);
        role.setActive(false);
        Role updatedRole = roleDao.save(role);
        return RoleBeanMapper.mapEntityToDto(updatedRole);
    }

    private void validIfSuperAdmin(Role role) {
        if (role.getName().equalsIgnoreCase("SUPER_ADMIN")) {
            throw new NotAllowedCustomException(
                    "Cannot modify system role: " + role.getName()
            );
        }
    }


    /**
     * Guard used by callers before creating or modifying a role: rejects any
     * request that asks for a system role, since only a super admin may define
     * those.
     *
     * @param requestDto the incoming role request to validate
     * @throws NotAllowedCustomException if the request flags the role as a system
     *                                   role
     */
    public void checkRoleCreationAndModificationAllowed(RoleRequestDto requestDto) {
        if (requestDto.getSystemRole() != null && requestDto.getSystemRole()) {
            throw new NotAllowedCustomException("Only Super Admin can create system roles");
        }
    }

    private Role getRoleByIdValidation(Long id) {
        Role role = roleDao.findById(id);
        if (role == null) {
            throw new ResourceNotFoundException("Role not found with id: " + id);
        }
        return role;
    }
}