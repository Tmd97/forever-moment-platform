package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.AuthUserResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.common.dto.request.RoleRequestDto;
import com.forvmom.common.dto.response.RoleResponseDto;
import com.forvmom.core.services.AdminRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Admin endpoints for managing user roles.
 *
 * <p>
 * Mapped under {@code /admin/roles}. Read operations are open to any
 * authenticated admin caller, while the mutating operations (create, update,
 * delete, activate, deactivate) additionally require the {@code SUPER_ADMIN}
 * role. Note that the delete endpoint currently removes the role row outright
 * even though its response message mentions deactivation.
 */
@RestController
@RequestMapping("/admin/roles")
@Tag(name = "Admin Role API", description = "Endpoints for managing user roles (Admin only)")
public class AdminRoleController {

    @Autowired
    private AdminRoleService roleService;

    /**
     * Creates a new role.
     *
     * @param requestDto the role definition to persist
     * @return {@code 201 CREATED} wrapping the created {@link RoleResponseDto}
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Role", description = "Create a new user role")
    public ResponseEntity<ApiResponse<?>> createRole(@Valid @RequestBody RoleRequestDto requestDto) {
        RoleResponseDto response = roleService.createRole(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, "Role created successfully"));
    }

    /**
     * Returns a role by its identifier.
     *
     * @param id identifier of the role
     * @return {@code 200 OK} wrapping the {@link RoleResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Role by ID", description = "Fetch a role by its unique ID")
    public ResponseEntity<ApiResponse<?>> getRoleById(@PathVariable Long id) {
        RoleResponseDto response = roleService.getRoleById(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns a role by its unique name.
     *
     * @param name name of the role, for example {@code SUPER_ADMIN}
     * @return {@code 200 OK} wrapping the {@link RoleResponseDto}
     */
    @GetMapping("/name/{name}")
    @Operation(summary = "Get Role by Name", description = "Fetch a role by its name")
    public ResponseEntity<ApiResponse<?>> getRoleByName(@PathVariable String name) {
        RoleResponseDto response = roleService.getRoleByName(name);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists every role, active or not.
     *
     * @return {@code 200 OK} wrapping the list of {@link RoleResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Roles", description = "Fetch a list of all roles")
    public ResponseEntity<ApiResponse<?>> getAllRoles() {
        List<RoleResponseDto> responses = roleService.getAllRoles();
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(responses, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists only the roles that are currently active and therefore assignable.
     *
     * @return {@code 200 OK} wrapping the list of {@link RoleResponseDto}
     */
    @GetMapping("/active")
    @Operation(summary = "Get Active Roles", description = "Fetch a list of all active roles")
    public ResponseEntity<ApiResponse<?>> getAllActiveRoles() {
        List<RoleResponseDto> responses = roleService.getAllActiveRoles();
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(responses, "Active roles fetched successfully"));
    }

    /**
     * Lists the built-in system roles, i.e. those flagged as system roles at
     * creation time.
     *
     * @return {@code 200 OK} wrapping the list of {@link RoleResponseDto}
     */
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<?>> getSystemRoles() {
        List<RoleResponseDto> responses = roleService.getSystemRoles();
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(responses, "System roles fetched successfully"));
    }

    /**
     * Updates a role's name, description, permission level or active flag. Requires
     * the {@code SUPER_ADMIN} role.
     *
     * @param id         identifier of the role to update
     * @param requestDto the new role values
     * @return {@code 200 OK} wrapping the updated {@link RoleResponseDto}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDto requestDto) {
        RoleResponseDto response = roleService.updateRole(id, requestDto);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(response, "Role updated successfully"));
    }

    /**
     * Deletes a role. Requires the {@code SUPER_ADMIN} role.
     *
     * <p>
     * The service performs a hard delete of the role row, so the "deactivated"
     * wording in the response message does not reflect the current behaviour.
     *
     * @param id identifier of the role to delete
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, "Role deleted (deactivated) successfully"));
    }

    /**
     * Marks a role active again so it can be assigned. Requires the
     * {@code SUPER_ADMIN} role.
     *
     * @param id identifier of the role to activate
     * @return {@code 200 OK} wrapping the updated {@link RoleResponseDto}
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> activateRole(@PathVariable Long id) {
        RoleResponseDto response = roleService.activateRole(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(response, "Role activated successfully"));
    }

    /**
     * Marks a role inactive so it is no longer assignable. Requires the
     * {@code SUPER_ADMIN} role.
     *
     * @param id identifier of the role to deactivate
     * @return {@code 200 OK} wrapping the updated {@link RoleResponseDto}
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> deactivateRole(@PathVariable Long id) {
        RoleResponseDto response = roleService.deactivateRole(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(response, "Role deactivated successfully"));
    }

    /**
     * Lists the authentication accounts that currently hold a given role.
     *
     * @param roleId identifier of the role
     * @return {@code 200 OK} wrapping the list of {@link AuthUserResponseDto}
     */
    @GetMapping("/{roleId}/users")
    public ResponseEntity<ApiResponse<?>> getAllUserByRole(@PathVariable Long roleId) {
        List<AuthUserResponseDto> responses = roleService.getAllUserByRole(roleId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(responses, "All account details of User fetched successfully"));
    }

}