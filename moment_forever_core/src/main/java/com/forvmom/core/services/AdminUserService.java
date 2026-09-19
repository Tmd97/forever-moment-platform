package com.forvmom.core.services;

import com.forvmom.common.dto.response.AdminAppUserResponseDto;
import com.forvmom.common.dto.response.RoleResponseDto;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.common.dto.request.UserProfileRequestDto;
import com.forvmom.core.mapper.ApplicationUserBeanMapper;
import com.forvmom.core.mapper.RoleBeanMapper;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.AuthUserRoleDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.auth.AuthUserRole;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.security.dto.AuthResponse;
import com.forvmom.security.dto.RegisterRequestDto;
import com.forvmom.security.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Administrative service for managing application users (the profile side) and
 * their linked authentication accounts.
 *
 * <p>
 * An account is modelled as a pair: an {@link AuthUser} that holds credentials
 * and role assignments, and an {@link ApplicationUser} that holds the profile.
 * Creation is delegated to {@code AuthService#register}, which creates both
 * sides; this service then re-reads the profile so it can return an admin view.
 *
 * <p>
 * The class is annotated {@code @Transactional} at type level, so all public
 * methods run in a transaction; read paths additionally use
 * {@code readOnly = true} and rely on fetch-joined queries
 * ({@code findByIdWithAuthAndRoles}, {@code findAllWithAuthAndRoles}) to load
 * auth and roles in a single round trip.
 */
@Service
@Transactional
public class AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    @Autowired
    private ApplicationUserDao applicationUserDao;

    @Autowired
    private AuthUserDao authUserDao;

    @Autowired
    private AuthUserRoleDao authUserRoleDao;

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleDao roleDao;

    /**
     * Registers a new account through {@code AuthService} and returns the freshly
     * created profile in the admin view. The profile is re-read by auth user id
     * because registration creates the {@link ApplicationUser} row as a side
     * effect.
     *
     * @param request the registration payload (credentials, profile and role data)
     * @return the created user in the admin response shape, with
     *         {@code createdBy} populated from the auth response
     * @throws ResourceNotFoundException if registration succeeded but no profile
     *                                   row could be found for the new auth id
     */
    @Transactional
    public AdminAppUserResponseDto createUser(RegisterRequestDto request) {
        AuthResponse authResponse = authService.register(request);
        // fetch the application user details using the auth user id
        Optional<ApplicationUser> appUser = applicationUserDao.findByAuthUserId(authResponse.getUserId());
        if (appUser.isEmpty()) {
            throw new ResourceNotFoundException(
                    "User created but profile not found for auth id: " + authResponse.getUserId());
        }
        AdminAppUserResponseDto res = ApplicationUserBeanMapper.mapEntityToAdminDto(appUser.get());
        // do populate whatever required from auth response(afermath)
        res.setCreatedBy(authResponse.getAssignedBy());
        return res;
    }

    /**
     * Loads a single application user together with its auth record and roles.
     *
     * @param id the application user identifier
     * @return the user in the admin response shape
     * @throws ResourceNotFoundException if no user exists with the given id
     */
    @Transactional(readOnly = true)
    public AdminAppUserResponseDto getAppUserById(Long id) {
        ApplicationUser appUser = applicationUserDao.findByIdWithAuthAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("No User exist given Id exist " + id));
        return ApplicationUserBeanMapper.mapEntityToAdminDto(appUser);
    }

    /**
     * Looks up an application user by email address, ignoring case.
     *
     * @param email the email address to search for
     * @return the user in the admin response shape
     * @throws ResourceNotFoundException if no user exists with the given email
     */
    @Transactional(readOnly = true)
    public AdminAppUserResponseDto getAppUserByEmailId(String email) {
        Optional<ApplicationUser> appUser = applicationUserDao.findByEmailIgnoreCase(email);
        if (appUser.isEmpty()) {
            throw new ResourceNotFoundException("No User exist given email exist " + email);
        }
        return ApplicationUserBeanMapper.mapEntityToAdminDto(appUser.get());
    }

    /**
     * Lists all application users with their auth records and roles fetched in one
     * query.
     *
     * @return every user in the admin response shape
     * @throws ResourceNotFoundException if there are no users at all
     */
    @Transactional(readOnly = true)
    public List<AdminAppUserResponseDto> getAllAppUser() {
        // Use optimized query to fetch All Users + Auth + Roles
        List<ApplicationUser> applicationUsers = applicationUserDao.findAllWithAuthAndRoles();
        if (applicationUsers == null || applicationUsers.isEmpty()) {
            throw new ResourceNotFoundException("users doesn't exist");
        }
        return applicationUsers.stream()
                .map(ApplicationUserBeanMapper::mapEntityToAdminDto)
                .toList();
    }

    /**
     * Updates a user's profile fields and, when {@code roleId} is supplied,
     * replaces the user's entire role set with that single role.
     *
     * <p>
     * The role change is applied to the associated {@link AuthUser} in the managed
     * persistence context, so it is flushed when the transaction commits.
     *
     * @param userId  the application user identifier
     * @param userDto the updatable profile fields, optionally carrying a role id
     * @return the updated user in the admin response shape
     * @throws ResourceNotFoundException if the user does not exist, or if the
     *                                   requested role id is unknown
     */
    @Transactional
    public AdminAppUserResponseDto updateAppUser(Long userId,
                                                 UserProfileRequestDto userDto) {
        ApplicationUser existing = applicationUserDao.findById(userId);
        if (existing == null) {
            throw new ResourceNotFoundException("No such user for given Id exist " + userId);
        }
        // map only updatable fields
        ApplicationUserBeanMapper.mapDtoToEntity(userDto, existing);

        // Handle Role Update if provided
        if (userDto.getRoleId() != null) {
            Role role = roleDao.findById(userDto.getRoleId());
            if (role == null) {
                throw new ResourceNotFoundException("User Role not exist in the System");
            }

            AuthUser authUser = existing.getAuthUser();
            // Clear existing roles and add new one
            authUser.getUserRoles().clear();
            authUser.addRole(role);
            // AuthUser updates will be cascaded if ApplicationUser -> AuthUser cascade is
            // set,
            // or we might need to save AuthUser explicitly if not.
            // ApplicationUser has @OneToOne(optional = false) private AuthUser authUser;
            // It defaults to no cascade usually unless specified.
            // Let's save AuthUser explicitly to be safe, or just rely on Transactional.
            // Since we modified the collection of AuthUser, and AuthUser is a managed
            // entity (fetched via graph in findById possibly, or via getter),
            // changes should be flushed at transaction commit.
            // However, existing ApplicationUser fetch in updateAppUser is just findById.
            // applicationUserDao.findById(userId) might not fetch AuthUser eagerly?
            // ApplicationUser.java: @OneToOne(optional = false) ... private AuthUser
            // authUser; (Default EAGER for OneToOne).
            // So AuthUser is fetched.
            // AuthUser.java: @OneToMany(mappedBy = "authUser", fetch = FetchType.LAZY,
            // cascade = CascadeType.ALL) private Set<AuthUserRole> userRoles;
            // Accessing getUserRoles() might trigger lazy load.
            // Since we are in @Transactional, it should work.
        }

        ApplicationUser res = applicationUserDao.update(existing);
        return ApplicationUserBeanMapper.mapEntityToAdminDto(res);
    }

    // TODO: we can also add soft delete functionality here instead of hard delete,
    // as per requirement
    /*
     * This method deletes the user account along with the associated authentication
     * details.
     */
    // @Transactional
    // public void deleteUserAccount(Long userId) {
    // AuthUser authUser = authUserDao.findById(userId);
    // if (authUser == null) {
    // logger.warn("Delete account failed: User not found - {}", userId);
    // throw new CustomAuthException("User not found for id: " + userId);
    // }
    // authUserDao.delete(authUser);
    // logger.info("User account deleted successfully for userId: {}", userId);
    // }

    /**
     * Deletes only the application user's profile row, leaving the associated
     * authentication account in place.
     *
     * @param userId the application user identifier
     * @throws CustomAuthException if no profile exists for the given id
     */
    @Transactional
    public void deleteUserProfile(Long userId) {
        ApplicationUser userProfile = applicationUserDao.findById(userId);
        if (userProfile == null) {
            logger.warn("Delete profile failed: AppUser not found - {}", userId);
            throw new CustomAuthException("AppUser not found for id: " + userId);
        }
        applicationUserDao.delete(userProfile);
        logger.info("User Profile deleted successfully for userId: {}", userId);
    }

    /**
     * Deletes the whole account: first the application user profile, then the
     * linked authentication record.
     *
     * @param userId the application user identifier
     * @throws CustomAuthException if no profile exists for the given id
     */
    @Transactional
    public void deleteAccount(Long userId) {
        ApplicationUser userProfile = applicationUserDao.findById(userId);
        if (userProfile == null) {
            logger.warn("Delete Account failed: AppUser not found - {}", userId);
            throw new CustomAuthException("AppUser not found for id: " + userId);
        }
        AuthUser authUser = userProfile.getAuthUser();
        // Soft delete the application user profile
        applicationUserDao.delete(userProfile);
        authUserDao.delete(authUser);

        // Note: AuthUser remains active but unlinked from profile (unless cascaded).
        // If AuthUser should be disabled or deleted, logic should be added here.
        // For now, adhering to soft delete of ApplicationUser.

        logger.info("User Account (Profile) deleted successfully for userId: {}", userId);
    }

    // @Transactional
    // public List<RoleResponseDto> getUserRoles(Long userId) {
    // List<AuthUserRole> authUserRoles = authUserRoleDao.findByAuthUserId(userId);
    // if (authUserRoles == null || authUserRoles.isEmpty()) {
    // throw new ResourceNotFoundException("No roles found for user id: " + userId);
    // }
    // List<RoleResponseDto> responseDtos = authUserRoles.stream()
    // .map(authUserRole -> authUserRole.getRole())
    // .map(RoleBeanMapper::mapEntityToDto).collect(Collectors.toList());
    //
    // return responseDtos;
    // }

    /**
     * Returns the roles currently assigned to an application user.
     *
     * @param appUserId the application user identifier
     * @return the assigned roles, or an empty list when the user has no auth record
     *         or no role assignments
     * @throws ResourceNotFoundException if no user exists with the given id
     */
    @Transactional
    public List<RoleResponseDto> getRolesByAppUserId(Long appUserId) {
        ApplicationUser appUser = applicationUserDao.findByIdWithAuthAndRoles(appUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No user found for id: " + appUserId));

        if (appUser.getAuthUser() == null || appUser.getAuthUser().getUserRoles() == null) {
            return List.of();
        }

        return appUser.getAuthUser().getUserRoles().stream()
                .map(AuthUserRole::getRole)
                .map(RoleBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }
}
