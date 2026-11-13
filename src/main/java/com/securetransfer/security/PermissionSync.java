package com.securetransfer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Syncs the permission catalogue ({@link PermissionCode}) and default roles into the
 * database on every start. Idempotent and never deletes:
 * <ul>
 *   <li>missing permissions are inserted;</li>
 *   <li>permissions removed from code are marked deprecated (not deleted, they may still be assigned);</li>
 *   <li>CUSTOMER and OPERATIONS roles are created if missing. OPERATIONS receives default
 *       permissions only when first created, so later admin changes are never overwritten.</li>
 * </ul>
 */
@Component
@Order(1)
public class PermissionSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionSync.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionSync(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncPermissions();
        ensureRole(RoleNames.CUSTOMER, Set.of());
        ensureRole(RoleNames.OPERATIONS, Set.of(PermissionCode.values()));
    }

    private void syncPermissions() {
        Set<String> codesInCode = Arrays.stream(PermissionCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        for (PermissionCode code : PermissionCode.values()) {
            permissionRepository.findByCode(code.name()).ifPresentOrElse(
                    existing -> {
                        existing.setDeprecated(false);
                        existing.setDescription(code.description());
                    },
                    () -> {
                        permissionRepository.save(new Permission(code.name(), code.description()));
                        log.info("Permission created: {}", code);
                    });
        }

        permissionRepository.findAll().stream()
                .filter(p -> !codesInCode.contains(p.getCode()) && !p.isDeprecated())
                .forEach(p -> {
                    p.setDeprecated(true);
                    log.warn("Permission {} no longer exists in code; marked deprecated", p.getCode());
                });
    }

    private void ensureRole(String name, Set<PermissionCode> defaultPermissions) {
        if (roleRepository.findByName(name).isPresent()) {
            return;
        }
        Role role = new Role(name);
        defaultPermissions.forEach(code -> role.getPermissions().add(
                permissionRepository.findByCode(code.name()).orElseThrow()));
        roleRepository.save(role);
        log.info("Role created: {} with permissions {}", name, defaultPermissions);
    }
}
