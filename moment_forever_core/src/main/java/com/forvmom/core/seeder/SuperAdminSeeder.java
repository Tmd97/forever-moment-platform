package com.forvmom.core.seeder;

import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.dto.RegisterRequestDto;
import com.forvmom.security.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps the initial {@code SUPER_ADMIN} account on application startup.
 *
 * <p>
 * Without this the system would ship with no way to authenticate as an
 * administrator and therefore no way to create the first one. The seeder is
 * idempotent — it checks for the account by username first, so restarts do not
 * duplicate or reset it.
 *
 * <p>
 * Seeding failures are logged rather than rethrown: a broken seed must not
 * prevent the service from starting and serving read traffic.
 *
 * <p>
 * <strong>⚠️ The seeded credentials are development defaults and must be changed
 * in any shared or production environment.</strong>
 */
@Component
public class SuperAdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final AuthService authService;
    private final RoleDao roleDao;
    private final AuthUserDao authUserDao;

    @Autowired
    public SuperAdminSeeder(AuthService authService, RoleDao roleDao, AuthUserDao authUserDao) {
        this.authService = authService;
        this.roleDao = roleDao;
        this.authUserDao = authUserDao;
    }

    /**
     * Creates the default {@code SUPER_ADMIN} user if it does not already exist.
     *
     * @param args standard command-line arguments (unused)
     * @throws Exception if the {@code SUPER_ADMIN} role itself is missing
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String superAdminEmail = "superadmin@cherishx.com";

        if (!authUserDao.existsByUsername(superAdminEmail)) {
            logger.info("Creating default SUPER_ADMIN user: {}", superAdminEmail);

            Role superAdminRole = roleDao.findByNameIgnoreCase("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Role SUPER_ADMIN is not found."));

            RegisterRequestDto request = new RegisterRequestDto();
            request.setEmail(superAdminEmail);
            request.setPassword("SuperAdmin@123"); // Strong password
            request.setFullName("Super Administrator");
            request.setRoleId(superAdminRole.getId());
            request.setPreferredCity("Headquarters");

            try {
                authService.register(request);
                logger.info("SUPER_ADMIN user created successfully.");
            } catch (Exception e) {
                logger.error("Failed to create SUPER_ADMIN user: {}", e.getMessage());
            }
        } else {
            logger.info("SUPER_ADMIN user already exists.");
        }
    }
}
