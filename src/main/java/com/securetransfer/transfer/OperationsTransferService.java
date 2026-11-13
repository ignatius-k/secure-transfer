package com.securetransfer.transfer;

import com.securetransfer.error.BadRequestException;
import com.securetransfer.security.CurrentUser;
import com.securetransfer.security.PermissionCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Staff use case: caller specifies both accounts. Each transfer type requires its own
 * permission, so e.g. a reconciliation officer cannot post agent transfers.
 */
@Service
public class OperationsTransferService {

    private final TransferEngine transferEngine;
    private final MetadataValidator metadataValidator;

    public OperationsTransferService(TransferEngine transferEngine, MetadataValidator metadataValidator) {
        this.transferEngine = transferEngine;
        this.metadataValidator = metadataValidator;
    }

    public TransferResult transfer(CurrentUser user, OperationsTransferRequest request) {
        PermissionCode required = request.type().requiredPermission();
        if (required == null) {
            throw BadRequestException.transferTypeNotAllowed(request.type().name());
        }
        if (!user.hasPermission(required)) {
            throw new AccessDeniedException("Missing permission " + required);
        }
        metadataValidator.validate(request.metadata());

        TransferCommand command = new TransferCommand(
                request.transactionRef(),
                request.type(),
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                null,
                request.reason(),
                user.username(),
                request.metadata());

        TransferResult result = transferEngine.execute(command);
        if (!result.isSuccess()) {
            throw new TransferFailedException(result.transaction(), result.replayed());
        }
        return result;
    }
}
