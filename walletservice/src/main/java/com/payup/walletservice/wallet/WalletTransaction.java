package com.payup.walletservice.wallet;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("wallet_transactions")
public class WalletTransaction {

    @Id
    private Long id;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private String description;
    private Instant createdAt;

    public WalletTransaction() {}

    private WalletTransaction(Long walletId, String type, BigDecimal amount, String description) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public static WalletTransaction credit(Long walletId, BigDecimal amount, String description) {
        return new WalletTransaction(walletId, "CREDIT", amount, description);
    }

    public static WalletTransaction debit(Long walletId, BigDecimal amount, String description) {
        return new WalletTransaction(walletId, "DEBIT", amount, description);
    }

    public Long getId() { return id; }
    public Long getWalletId() { return walletId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
