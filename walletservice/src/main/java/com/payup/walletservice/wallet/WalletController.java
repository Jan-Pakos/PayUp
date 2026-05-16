package com.payup.walletservice.wallet;

import com.payup.walletservice.auth.AuthenticatedUser;
import com.payup.walletservice.wallet.dto.AmountRequest;
import com.payup.walletservice.wallet.dto.TransactionResponse;
import com.payup.walletservice.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse getWallet() {
        return WalletResponse.from(walletService.getOrCreateWallet(currentUserId()));
    }

    @PostMapping("/deposit")
    public WalletResponse deposit(@Valid @RequestBody AmountRequest request) {
        return WalletResponse.from(walletService.deposit(currentUserId(), request.amount(), request.description()));
    }

    @PostMapping("/withdraw")
    public WalletResponse withdraw(@Valid @RequestBody AmountRequest request) {
        return WalletResponse.from(walletService.withdraw(currentUserId(), request.amount(), request.description()));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getTransactions() {
        return walletService.getTransactions(currentUserId()).stream()
            .map(TransactionResponse::from)
            .toList();
    }

    private Long currentUserId() {
        return ((AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).userId();
    }
}
