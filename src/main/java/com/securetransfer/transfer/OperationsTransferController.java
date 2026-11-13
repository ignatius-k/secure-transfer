package com.securetransfer.transfer;

import com.securetransfer.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Staff transfer: gated by permission. The required permission depends on the transfer type. */
@RestController
@RequestMapping("/api/v1/operations/transfers")
public class OperationsTransferController {

    private final OperationsTransferService operationsTransferService;
    private final TransferResponseMapper mapper;

    public OperationsTransferController(OperationsTransferService operationsTransferService,
                                        TransferResponseMapper mapper) {
        this.operationsTransferService = operationsTransferService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TRANSFER_RECONCILE', 'TRANSFER_AGENT')")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody OperationsTransferRequest request,
                                                     Authentication authentication) {
        TransferResult result = operationsTransferService.transfer(CurrentUser.from(authentication), request);
        return TransferController.created(mapper.staffView(result.transaction()), result.replayed());
    }
}
