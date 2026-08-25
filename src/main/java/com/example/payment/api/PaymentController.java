package com.example.payment.api;

import com.example.payment.domain.Account;
import com.example.payment.domain.LedgerEntry;
import com.example.payment.domain.Transfer;
import com.example.payment.util.ApiBadRequestException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody PaymentRequests.CreateAccount request) {
        Account account = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getId()))
                .body(service.toResponse(account));
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return service.getAccountResponse(id);
    }

    @GetMapping("/accounts/{id}/balance")
    public AccountBalanceResponse getBalance(@PathVariable Long id) {
        return service.getBalance(id);
    }

    @GetMapping("/accounts/{id}/transactions")
    public AccountTransactionPage getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page and size must be valid");
        }
        return service.getTransactions(id, page, size);
    }

    @PatchMapping("/accounts/{id}/status")
    public AccountResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PaymentRequests.UpdateStatus request) {
        return service.toResponse(service.updateStatus(id, request.status()));
    }

    @PostMapping("/accounts/{id}/deposit")
    public DepositResponse deposit(@PathVariable Long id, @Valid @RequestBody PaymentRequests.Money request) {
        return service.deposit(id, request.amount());
    }

    @PostMapping("/accounts/{id}/withdraw")
    public DepositResponse withdraw(@PathVariable Long id, @Valid @RequestBody PaymentRequests.Money request) {
        return service.withdraw(id, request.amount());
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody PaymentRequests.Transfer request) {
        if (key == null || key.isBlank()) {
            throw new ApiBadRequestException("Idempotency-Key header is required");
        }
        if (key.length() > 64) {
            throw new ApiBadRequestException("Idempotency-Key must be at most 64 characters");
        }
        Transfer transfer = service.transfer(key, request);
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + transfer.getId()))
                .body(service.toResponse(transfer));
    }

    @GetMapping("/transfers/{id}")
    public TransferResponse getTransfer(@PathVariable Long id) {
        return service.toResponse(service.getTransfer(id));
    }
}