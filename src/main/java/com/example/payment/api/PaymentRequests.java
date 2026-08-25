package com.example.payment.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class PaymentRequests {
    private PaymentRequests() { }

    public record CreateAccount(
            @NotBlank @Size(max = 120) String ownerName,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin(value = "0.00") BigDecimal initialBalance
    ) { }

    public record UpdateStatus(@NotBlank @Pattern(regexp = "ACTIVE|FROZEN|CLOSED") String status) { }

    public record Money(@NotNull @DecimalMin(value = "0.0001") BigDecimal amount) { }

    public record Transfer(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
    ) { }
}
