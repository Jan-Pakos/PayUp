package com.payup.walletservice.wallet;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface WalletRepository extends CrudRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
}
