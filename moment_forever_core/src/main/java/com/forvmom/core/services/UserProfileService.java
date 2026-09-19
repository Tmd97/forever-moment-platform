package com.forvmom.core.services;

import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.dto.response.AppUserResponseDto;
import com.forvmom.common.dto.request.UserProfileRequestDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.ApplicationUserBeanMapper;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RefreshTokenDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.data.entities.auth.RefreshToken;
import com.forvmom.security.config.PasswordConfig;
import com.forvmom.security.dto.JwtUserDetails;
import com.forvmom.security.service.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

/**
 * Self-service profile operations for the currently authenticated user.
 *
 * <p>
 * Every method resolves the caller from the Spring Security context rather than
 * accepting a user id, so one user can never act on another's profile.
 *
 * <p>
 * Identity is split across two entities: {@code AuthUser} holds credentials while
 * {@link ApplicationUser} holds profile data. Anything touching the login
 * identifier must therefore update both, which is why the email change path below
 * writes to {@code AuthUser} as well.
 */
@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private ApplicationUserDao applicationUserDao;

    @Autowired
    private AuthUserDao authUserDao;

    @Autowired
    private RefreshTokenDao refreshTokenDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordConfig passwordEncoder;

    /**
     * Updates the authenticated user's profile.
     *
     * <p>
     * A changed email is also the login username, so it is propagated to the
     * {@code AuthUser} record and rejected if already taken. Both writes share this
     * transaction to avoid the profile and credentials disagreeing.
     *
     * @param userProfileRequestDto new profile values
     * @return the updated profile
     * @throws ResourceNotFoundException if the principal has no application user
     * @throws CustomAuthException       if the requested email is already in use
     */
    @Transactional
    public AppUserResponseDto updateCurrentUserProfile(@Valid UserProfileRequestDto userProfileRequestDto) {
        Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (o instanceof UserDetails) {
            JwtUserDetails jwtUserDetails = (JwtUserDetails) o;
            Optional<ApplicationUser> applicationUser = applicationUserDao.findByAuthUserId(jwtUserDetails.getId());
            if (applicationUser.isEmpty()) {
                throw new ResourceNotFoundException("User doesn't exist in System");
            }


            // Update fields
            applicationUser.get().setFullName(userProfileRequestDto.getFullName());

            // Sync email change with AuthUser
            if (!applicationUser.get().getEmail().equalsIgnoreCase(userProfileRequestDto.getEmail())) {
                if (authUserDao.existsByUsername(userProfileRequestDto.getEmail())) {
                    throw new CustomAuthException("Email already in use: " + userProfileRequestDto.getEmail());
                }
                AuthUser authUser = applicationUser.get().getAuthUser();
                authUser.setUsername(userProfileRequestDto.getEmail());
                authUserDao.save(authUser);
                applicationUser.get().setEmail(userProfileRequestDto.getEmail());
            }

            applicationUser.get().setPhoneNumber(userProfileRequestDto.getPhoneNumber());
            applicationUser.get().setProfilePictureUrl(userProfileRequestDto.getProfilePictureUrl());
            applicationUser.get().setDateOfBirth(userProfileRequestDto.getDateOfBirth());
            applicationUser.get().setPreferredCity(userProfileRequestDto.getPreferredCity());

            // Save updated user
            ApplicationUser updatedUser = applicationUserDao.update(applicationUser.get());
            return ApplicationUserBeanMapper.mapEntityToDto(updatedUser);
        }
        throw new RuntimeException("User not authenticated");
    }

    /**
     * Returns the authenticated user's profile.
     *
     * @return the current user's profile
     * @throws CustomAuthException       if there is no authenticated principal or
     *                                   it is of an unexpected type
     * @throws ResourceNotFoundException if the principal has no application user
     */
    public AppUserResponseDto getCurrentUserProfile() {
        // Add null check
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomAuthException("No authenticated user");
        }

        Object o = authentication.getPrincipal();
        if (!(o instanceof UserDetails)) { // Check type before casting
            throw new CustomAuthException("Invalid principal type");
        }

        JwtUserDetails jwtUserDetails = (JwtUserDetails) o;
        Optional<ApplicationUser> applicationUser = applicationUserDao.findByAuthUserId(jwtUserDetails.getId());
        if (applicationUser.isEmpty()) {
            throw new ResourceNotFoundException("User doesn't exist in System");
        }
        return ApplicationUserBeanMapper.mapEntityToDto(applicationUser.get());
    }

    /**
     * Permanently deletes the authenticated user's account.
     *
     * <p>
     * Deletion targets the {@code AuthUser}; the {@link ApplicationUser} is removed
     * by cascade. The password is re-verified first because a valid session alone
     * is not sufficient authorisation for an irreversible action.
     *
     * @param password the caller's current password, for re-authentication
     * @throws CustomAuthException       if unauthenticated or the password is wrong
     * @throws ResourceNotFoundException if the principal has no application user
     */
    public void deleteCurrentUserProfile(String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomAuthException("No authenticated user");
        }

        Object o = authentication.getPrincipal();
        if (!(o instanceof UserDetails)) { // Check type before casting
            throw new CustomAuthException("Invalid principal type");
        }

        JwtUserDetails jwtUserDetails = (JwtUserDetails) o;
        Optional<ApplicationUser> applicationUser = applicationUserDao.findByAuthUserId(jwtUserDetails.getId());

        if (applicationUser.isEmpty()) {
            throw new ResourceNotFoundException("User doesn't exist in System");
        }
        // Verify password before deletion
        if (!passwordEncoder.passwordEncoder().matches(password, jwtUserDetails.getPassword())) {
            throw new CustomAuthException("Invalid password");
        }
        authUserDao.delete(applicationUser.get().getAuthUser());
    }

    /**
     * Deactivates the authenticated user's account without deleting it.
     *
     * <p>
     * Locking the account alone would not end the session, because existing refresh
     * tokens could still mint new access tokens. All outstanding refresh tokens are
     * therefore revoked in the same transaction.
     *
     * @param refreshToken the caller's current refresh token (currently unused;
     *                     all of the user's tokens are revoked)
     * @throws CustomAuthException       if the principal is of an unexpected type
     * @throws ResourceNotFoundException if the principal has no application user
     */
    @Transactional
    public void deactivateCurrentAccount(String refreshToken) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Object o = authentication.getPrincipal();
        if (!(o instanceof UserDetails)) {
            throw new CustomAuthException("Invalid principal type");
        }
        JwtUserDetails jwtUserDetails = (JwtUserDetails) o;
        Optional<ApplicationUser> applicationUser = applicationUserDao.findByAuthUserId(jwtUserDetails.getId());
        if (applicationUser.isEmpty()) {
            throw new ResourceNotFoundException("User doesn't exist in System");
        }
        AuthUser authUser = applicationUser.get().getAuthUser();
        authUser.setAccountNonLocked(false);
        authUserDao.update(authUser);

        // Revoke all refresh tokens for this user
        List<RefreshToken> tokens = refreshTokenDao
                .findByAuthUserIdAndRevokedFalse(authUser.getId());
        tokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenDao.save(token);
        });
        logger.info("User account deActivated successfully for userId: {}", authUser.getUsername());
    }
}
