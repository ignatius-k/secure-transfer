package com.securetransfer.account;

import com.securetransfer.error.NotFoundException;
import com.securetransfer.security.CurrentUser;
import com.securetransfer.security.PermissionCode;
import com.securetransfer.security.RoleNames;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountController(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasAuthority('ACCOUNT_READ')")
    @Transactional(readOnly = true)
    public AccountResponse get(@PathVariable String accountNumber, Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(NotFoundException::account);

        if (!user.hasPermission(PermissionCode.ACCOUNT_READ)) {
            if (!user.hasRole(RoleNames.CUSTOMER)) {
                throw new AccessDeniedException("Not allowed to view accounts");
            }
            // Someone else's account -> 404, so customers cannot probe which accounts exist.
            if (!user.userId().equals(account.getCustomerId())) {
                throw NotFoundException.account();
            }
        }
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountName(),
                account.getStatus(),
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                Instant.now(clock));
    }
}
