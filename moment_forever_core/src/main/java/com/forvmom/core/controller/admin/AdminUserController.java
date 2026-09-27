package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.response.AdminAppUserResponseDto;
import com.forvmom.common.dto.response.RoleResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.common.dto.request.UserProfileRequestDto;
import com.forvmom.core.services.AdminUserService;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.dto.RegisterRequestDto;
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
 * Admin endpoints for managing other users' accounts and profiles.
 *
 * <p>
 * Mapped under {@code /admin/user}. Unlike the self-service user profile API,
 * every operation here addresses a user by explicit id. Account creation is
 * restricted to the {@code SUPER_ADMIN} role; the remaining endpoints are open
 * to any caller the API Gateway has authenticated for this admin path.
 */
@RestController
@RequestMapping("/admin/user")
@Tag(name = "Admin User API", description = "Endpoints for managing users (Admin/SuperAdmin only)")
public class AdminUserController {
        @Autowired
        private AdminUserService appUserService;

        /**
         * Creates a new user account together with its profile. Requires the
         * {@code SUPER_ADMIN} role.
         *
         * @param request the registration details for the new account
         * @return {@code 201 CREATED} wrapping the created
         *         {@link AdminAppUserResponseDto}
         */
        @PostMapping("/create")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        @Operation(summary = "Create User (Super Admin)", description = "Create a new user account (Requires Super Admin role)")
        public ResponseEntity<ApiResponse<?>> createUser(
                        @Valid @RequestBody RegisterRequestDto request) {
                AdminAppUserResponseDto response = appUserService
                                .createUser(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ResponseUtil.buildCreatedResponse(response,
                                                "User created successfully by Super Admin"));
        }

        /**
         * Returns a user's profile plus its account details (credentials flags and
         * assigned roles).
         *
         * @param userId identifier of the application user
         * @return {@code 200 OK} wrapping the {@link AdminAppUserResponseDto}
         */
        @GetMapping("/profile/{userId}")
        @Operation(summary = "Get User Profile", description = "Fetch a user's profile by ID")
        public ResponseEntity<ApiResponse<?>> getUserProfile(@PathVariable Long userId) {
                AdminAppUserResponseDto appUserResponseDto = appUserService
                                .getAppUserById(userId);
                return ResponseEntity.ok(
                                ResponseUtil.buildOkResponse(appUserResponseDto, AppConstants.MSG_FETCHED));
        }

        // TODO: currently this is for fetching user profile by any unique fields
//        @GetMapping("/profile/{}")
//        public ResponseEntity<ApiResponse<?>> getUserProfileByEmail(
//                        @Valid @RequestBody UserProfileRequestDto userProfileRequestDto) {
//                AdminAppUserResponseDto res = appUserService
//                                .getAppUserByEmailId(userProfileRequestDto.getEmail());
//                return ResponseEntity.ok(
//                                ResponseUtil.buildOkResponse(res, AppConstants.MSG_FETCHED));
//        }

        /**
         * Updates another user's profile fields.
         *
         * @param userId  identifier of the application user to update
         * @param userDto the new profile values
         * @return {@code 200 OK} wrapping the updated
         *         {@link AdminAppUserResponseDto}
         */
        @PutMapping("/profile/{userId}")
        public ResponseEntity<ApiResponse<?>> updateUserProfile(
                        @PathVariable Long userId,
                        @Valid @RequestBody UserProfileRequestDto userDto) {
                AdminAppUserResponseDto updatedUser = appUserService
                                .updateAppUser(userId, userDto);
                return ResponseEntity.ok(
                                ResponseUtil.buildCreatedResponse(updatedUser, "User profile updated"));
        }

        /**
         * Lists all user profiles with their account details.
         *
         * @return {@code 200 OK} wrapping the list of
         *         {@link AdminAppUserResponseDto}
         */
        @GetMapping("/profiles")
        @Operation(summary = "Get All User Profiles", description = "Fetch all user profiles")
        public ResponseEntity<ApiResponse<?>> getUserProfiles() {
                List<AdminAppUserResponseDto> appUserResponseDto = appUserService
                                .getAllAppUser();
                return ResponseEntity.ok(
                                ResponseUtil.buildOkResponse(appUserResponseDto, AppConstants.MSG_FETCHED));
        }

        /**
         * Deletes a user's profile while leaving the underlying authentication
         * account in place.
         *
         * @param userId identifier of the application user
         * @return {@code 201 CREATED} with an empty payload
         */
        @DeleteMapping("/profile/{userId}")
        public ResponseEntity<ApiResponse<?>> deleteProfile(@PathVariable Long userId) {
                appUserService.deleteUserProfile(userId);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseUtil.buildCreatedResponse(null, "User Profile deleted successfully"));
        }

        /**
         * Deletes a user's profile and its authentication account.
         *
         * <p>
         * The parameter is not annotated, so Spring binds it from a request
         * parameter named {@code userId} rather than from the request body.
         *
         * @param userId identifier of the application user
         * @return {@code 201 CREATED} with an empty payload
         */
        @PostMapping("/deleteAccount")
        public ResponseEntity<ApiResponse<?>> deleteAccount(Long userId) {
                appUserService.deleteAccount(userId);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseUtil.buildOkResponse(null, "User Profile deleted successfully"));
        }

        /**
         * Lists the roles assigned to a user.
         *
         * @param userId identifier of the application user
         * @return {@code 200 OK} wrapping the list of {@link RoleResponseDto}
         */
        @GetMapping("/{userId}/roles")
        @Operation(summary = "Get User Roles", description = "Fetch all roles assigned to a user")
        public ResponseEntity<ApiResponse<?>> getUserRoles(@PathVariable Long userId) {
                List<RoleResponseDto> roleResponseDtos = appUserService.getRolesByAppUserId(userId);
                return ResponseEntity.ok(
                                ResponseUtil.buildOkResponse(roleResponseDtos, "User roles fetched successfully"));
        }

}
