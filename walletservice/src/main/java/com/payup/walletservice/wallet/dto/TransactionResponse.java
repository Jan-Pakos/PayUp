package com.payup.walletservice.wallet.dto;

import com.payup.walletservice.wallet.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, String type, BigDecimal amount, String description, Instant createdAt) {

    public static TransactionResponse from(WalletTransaction tx) {
        return new TransactionResponse(tx.getId(), tx.getType(), tx.getAmount(), tx.getDescription(), tx.getCreatedAt());
    }
}
