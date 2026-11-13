package com.securetransfer.config;

import com.securetransfer.account.Account;
import com.securetransfer.account.AccountRepository;
import com.securetransfer.account.AccountStatus;
import com.securetransfer.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Local/demo data only (app.demo-data.enabled). Runs after PermissionSync so roles exist.
 * Seeds once: skipped if any user already exists.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Password@123";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, RoleRepository roleRepository,
                          PermissionRepository permissionRepository, AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        Role customer = roleRepository.findByName(RoleNames.CUSTOMER).orElseThrow();
        Role operations = roleRepository.findByName(RoleNames.OPERATIONS).orElseThrow();
        Role reconOfficer = reconOfficerRole();

        User jane = user("jane.doe", customer);
        User john = user("john.smith", customer);
        User peter = user("peter.okello", customer);
        user("ops.mary", operations);
        user("recon.paul", reconOfficer);

        account("1000000001", "Jane Doe", jane.getId(), "50000.00", AccountStatus.ACTIVE);
        account("1000000002", "John Smith", john.getId(), "10000.00", AccountStatus.ACTIVE);
        account("1000000003", "Peter Okello", peter.getId(), "5000.00", AccountStatus.INACTIVE);
        account("9000000001", "Reconciliation Suspense", null, "1000000.00", AccountStatus.ACTIVE);

        log.info("Demo data seeded (password for all demo users: {})", DEMO_PASSWORD);
    }

    /** Demonstrates per-type permissions: can reconcile, cannot post agent transfers. */
    private Role reconOfficerRole() {
        Role role = new Role("RECON_OFFICER");
        role.getPermissions().add(permissionRepository.findByCode(PermissionCode.TRANSFER_RECONCILE.name()).orElseThrow());
        role.getPermissions().add(permissionRepository.findByCode(PermissionCode.TRANSFER_READ.name()).orElseThrow());
        return roleRepository.save(role);
    }

    private User user(String username, Role role) {
        User user = new User(username, passwordEncoder.encode(DEMO_PASSWORD));
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private void account(String number, String name, Long customerId, String balance, AccountStatus status) {
        accountRepository.save(new Account(number, name, customerId, new BigDecimal(balance), status));
    }
}
