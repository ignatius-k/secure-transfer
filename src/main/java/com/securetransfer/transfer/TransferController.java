package com.securetransfer.transfer;

import com.securetransfer.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final CustomerTransferService customerTransferService;
    private final TransferQueryService transferQueryService;
    private final TransferResponseMapper mapper;

    public TransferController(CustomerTransferService customerTransferService,
                              TransferQueryService transferQueryService,
                              TransferResponseMapper mapper) {
        this.customerTransferService = customerTransferService;
        this.transferQueryService = transferQueryService;
        this.mapper = mapper;
    }

    /** Customer transfer: gated by role. Source account comes from the token. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody CustomerTransferRequest request,
                                                     @RequestHeader(value = "X-Channel", required = false) String channel,
                                                     Authentication authentication) {
        var outcome = customerTransferService.transfer(CurrentUser.from(authentication), request, channel);
        TransferResponse body = mapper.customerView(outcome.result().transaction(), outcome.customerAccountNumber());
        return created(body, outcome.result().replayed());
    }

    @GetMapping("/{transactionRef}")
    @PreAuthorize("hasRole('CUSTOMER') or hasAuthority('TRANSFER_READ')")
    public TransferResponse get(@PathVariable String transactionRef, Authentication authentication) {
        return transferQueryService.get(CurrentUser.from(authentication), transactionRef);
    }

    /** 201 + Location for a new posting; 200 + Idempotent-Replayed for a replay of the same request. */
    static ResponseEntity<TransferResponse> created(TransferResponse body, boolean replayed) {
        if (replayed) {
            return ResponseEntity.status(HttpStatus.OK).header(REPLAYED_HEADER, "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + body.transactionRef())).body(body);
    }
}
