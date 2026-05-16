package com.payup.walletservice.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
            .orElseGet(() -> walletRepository.save(new Wallet(userId, BigDecimal.ZERO, "USD")));
    }

    @Transactional
    public Wallet deposit(Long userId, BigDecimal amount, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.credit(amount);
        transactionRepository.save(WalletTransaction.credit(wallet.getId(), amount, description));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet withdraw(Long userId, BigDecimal amount, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        wallet.debit(amount);
        transactionRepository.save(WalletTransaction.debit(wallet.getId(), amount, description));
        return walletRepository.save(wallet);
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
