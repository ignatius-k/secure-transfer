package com.securetransfer.transfer;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** A transfer record. Written only by {@link TransferEngine}. */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 64)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(name = "source_account_number", nullable = false, length = 20)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 20)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 140)
    private String narration;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 50)
    private FailureReason failureReason;

    @Column(name = "initiated_by", nullable = false, length = 64)
    private String initiatedBy;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    /** Opaque caller context. Stored, never interpreted by the engine. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
    }

    static Transaction from(TransferCommand cmd, String requestHash, Instant now) {
        Transaction t = new Transaction();
        t.transactionRef = cmd.transactionRef();
        t.type = cmd.type();
        t.sourceAccountNumber = cmd.sourceAccountNumber();
        t.destinationAccountNumber = cmd.destinationAccountNumber();
        t.amount = cmd.amount();
        t.narration = cmd.narration();
        t.reason = cmd.reason();
        t.initiatedBy = cmd.initiatedBy();
        t.metadata = cmd.metadata();
        t.requestHash = requestHash;
        t.createdAt = now;
        return t;
    }

    void markSuccess() {
        this.status = TransactionStatus.SUCCESS;
        this.failureReason = null;
    }

    void markFailed(FailureReason reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean involves(String accountNumber) {
        return accountNumber.equals(sourceAccountNumber) || accountNumber.equals(destinationAccountNumber);
    }

    public Long getId() { return id; }
    public String getTransactionRef() { return transactionRef; }
    public TransferType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getNarration() { return narration; }
    public String getReason() { return reason; }
    public FailureReason getFailureReason() { return failureReason; }
    public String getInitiatedBy() { return initiatedBy; }
    public String getRequestHash() { return requestHash; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
