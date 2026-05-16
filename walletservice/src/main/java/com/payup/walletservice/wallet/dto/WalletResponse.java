package com.payup.walletservice.wallet.dto;

import com.payup.walletservice.wallet.Wallet;

import java.math.BigDecimal;

public record WalletResponse(Long id, Long userId, BigDecimal balance, String currency) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance(), wallet.getCurrency());
    }
}
