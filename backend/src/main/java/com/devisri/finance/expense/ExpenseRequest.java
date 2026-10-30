package com.devisri.finance.expense;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Only these four fields can ever be set by the browser. Anything else it sends
 * (for example a different "user") is ignored.
 */
public record ExpenseRequest(
        @NotBlank(message = "Title is required") @Size(max = 100, message = "Title is too long") String title,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be more than 0") Double amount,
        @NotBlank(message = "Category is required") @Size(max = 30, message = "Category is too long") String category,
        LocalDate date) {
}
