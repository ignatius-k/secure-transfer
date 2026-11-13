package com.securetransfer.transfer;

import com.securetransfer.account.AccountRepository;
import com.securetransfer.error.NotFoundException;
import com.securetransfer.security.CurrentUser;
import com.securetransfer.security.PermissionCode;
import com.securetransfer.security.RoleNames;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferQueryService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransferResponseMapper mapper;

    public TransferQueryService(TransactionRepository transactionRepository,
                                AccountRepository accountRepository,
                                TransferResponseMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public TransferResponse get(CurrentUser user, String transactionRef) {
        Transaction transaction = transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(NotFoundException::transfer);

        if (user.hasPermission(PermissionCode.TRANSFER_READ)) {
            return mapper.staffView(transaction);
        }
        if (user.hasRole(RoleNames.CUSTOMER)) {
            String own = accountRepository.findByCustomerId(user.userId())
                    .orElseThrow(NotFoundException::transfer)
                    .getAccountNumber();
            // Not a party to the transfer -> 404, so customers cannot probe for other refs.
            if (!transaction.involves(own)) {
                throw NotFoundException.transfer();
            }
            return mapper.customerView(transaction, own);
        }
        throw new AccessDeniedException("Not allowed to view transfers");
    }
}
