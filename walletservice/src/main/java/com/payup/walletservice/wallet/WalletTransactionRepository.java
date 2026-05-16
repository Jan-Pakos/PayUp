package com.payup.walletservice.wallet;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WalletTransactionRepository extends CrudRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
