package com.forvmom.data.seeder;

import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.auth.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RoleDataSeeder - Creates initial system roles on application startup
 */
@Component
@Order(1)
public class RoleDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RoleDataSeeder.class);

    private final RoleDao roleDao;

    public RoleDataSeeder(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Starting role data initialization...");

        // 0. SUPER_ADMIN Role - Absolute access
        createRoleIfNotFound(
                "SUPER_ADMIN",
                "Super Administrator - absolute access",
                1000,
                true
        );

        // 1. USER Role - Basic customer role
        createRoleIfNotFound(
                "USER",
                "Regular customer - can book experiences and manage profile",
                10,
                true
        );

        // 2. ADMIN Role - Full system administrator
        createRoleIfNotFound(
                "ADMIN",
                "System administrator - full access to all features",
                100,
                true
        );

        // 3. CONTENT_MANAGER Role - Can manage experiences
        createRoleIfNotFound(
                "CONTENT_MANAGER",
                "Can create, edit, and manage experiences (services)",
                50,
                false
        );

        // 4. BOOKING_MANAGER Role - Can manage bookings
        createRoleIfNotFound(
                "BOOKING_MANAGER",
                "Can view, confirm, and cancel customer bookings",
                40,
                false
        );

        // 5. VENDOR Role - Vendor partner role
        createRoleIfNotFound(
                "VENDOR",
                "Vendor partner - manages own profile and services",
                20,
                true
        );

        logger.info("Role data initialization completed.");
    }

    private void createRoleIfNotFound(String name, String description,
            Integer permissionLevel, boolean systemRole) {

        boolean exists = roleDao.existsByNameIgnoreCase(name);

        if (!exists) {
            Role role = new Role(name, description);
            role.setPermissionLevel(permissionLevel);
            role.setSystemRole(systemRole);
            role.setActive(true);

            roleDao.save(role);

            logger.info("Created role: {} (Level: {}, System: {})",
                    name, permissionLevel, systemRole);
        } else {
            logger.debug("Role already exists: {}", name);
        }
    }
}