package com.forvmom.core.controller.user;

import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.dto.response.AppUserResponseDto;
import com.forvmom.common.dto.request.UserProfileRequestDto;
import com.forvmom.core.services.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Self-service profile endpoints for the signed-in end user.
 *
 * <p>
 * Mapped under {@code /user/profile}, so every request reaches this controller
 * already authenticated. No endpoint takes a user id: the target user is always
 * resolved from the security context by {@link UserProfileService}, which
 * prevents one user from acting on another's profile.
 */
@RestController
@RequestMapping("/user/profile")
@Tag(name = "User Profile API", description = "Endpoints for managing user profile")
public class UserProfileController {

    private final UserProfileService userService;

    /**
     * Creates the controller with its collaborating service.
     *
     * @param userService service that resolves and mutates the current user's
     *                    profile
     */
    @Autowired
    public UserProfileController(UserProfileService userService) {
        this.userService = userService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return {@code 200 OK} wrapping the caller's {@link AppUserResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get Current User Profile", description = "Fetch the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<?>> getMe() {
        AppUserResponseDto currentUser = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(currentUser, "User profile fetched"));
    }

    /**
     * Updates the authenticated user's profile. A changed email is also the login
     * username, so the service propagates it to the credentials record and rejects
     * it if already taken.
     *
     * @param userProfileRequestDto the new profile values
     * @return {@code 201 CREATED} wrapping the updated {@link AppUserResponseDto}
     */
    @PutMapping
    @Operation(summary = "Update User Profile", description = "Update the profile details of the currently authenticated user")
    public ResponseEntity<ApiResponse<?>> updateMe(@RequestBody @Valid UserProfileRequestDto userProfileRequestDto) {
        AppUserResponseDto updatedProfile = userService.updateCurrentUserProfile(userProfileRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(updatedProfile, "User profile updated"));
    }

    /**
     * Permanently deletes the authenticated user's account. The password is
     * re-verified by the service before deletion, since a valid session alone is
     * not enough for an irreversible action.
     *
     * @param confirmPassword the caller's current password, passed as a query
     *                        parameter
     * @return {@code 204 NO CONTENT} with no body
     */
    @DeleteMapping
    @Operation(summary = "Delete User Profile", description = "Permanently delete the current user's profile (Requires password confirmation)")
    public ResponseEntity<ApiResponse<?>> deleteMe(@RequestParam String confirmPassword) {
        userService.deleteCurrentUserProfile(confirmPassword);
        return ResponseEntity.noContent().build();
    }

    // delete account endpoint, which will invalidate all tokens for the user and
    // delete the user account
    // TODO: delete account can be delete via emailId, or username or refresh token
    /**
     * Deactivates the authenticated user's account instead of deleting it: the
     * account is locked and all of the user's outstanding refresh tokens are
     * revoked, so existing sessions cannot be renewed.
     *
     * <p>
     * The {@code Authorization} header value is forwarded to the service but is not
     * used to select the tokens; revocation covers every token of the caller.
     *
     * @param token the raw {@code Authorization} header of the request
     * @return {@code 201 CREATED} with an empty payload
     */
    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate Account", description = "Deactivate the current user's account")
    public ResponseEntity<ApiResponse<?>> deActivateAccount(
            @RequestHeader("Authorization") String token) {
        userService.deactivateCurrentAccount(token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildOkResponse(null, "Account deleted successfully"));
    }
}