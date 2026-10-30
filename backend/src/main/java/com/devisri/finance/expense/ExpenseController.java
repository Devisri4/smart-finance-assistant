package com.devisri.finance.expense;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devisri.finance.common.ApiExceptionHandler;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseRepository expenses;
    private final Clock clock;

    public ExpenseController(ExpenseRepository expenses, Clock clock) {
        this.expenses = expenses;
        this.clock = clock;
    }

    @GetMapping
    List<Expense> list(@AuthenticationPrincipal String userId) {
        return expenses.findByUserOrderByDateDescCreatedAtDesc(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Expense create(@AuthenticationPrincipal String userId, @Valid @RequestBody ExpenseRequest request) {
        return expenses.save(new Expense(userId, request.title().trim(), request.amount(),
                request.category().trim(), toInstant(request.date())));
    }

    @PatchMapping("/{id}")
    Expense update(@AuthenticationPrincipal String userId, @PathVariable String id,
                   @Valid @RequestBody ExpenseRequest request) {
        Expense expense = findOwned(userId, id);
        expense.setTitle(request.title().trim());
        expense.setAmount(request.amount());
        expense.setCategory(request.category().trim());
        expense.setDate(toInstant(request.date()));
        return expenses.save(expense);
    }

    @DeleteMapping("/{id}")
    Map<String, String> delete(@AuthenticationPrincipal String userId, @PathVariable String id) {
        expenses.delete(findOwned(userId, id));
        return Map.of("message", "Expense deleted");
    }

    private Expense findOwned(String userId, String id) {
        return expenses.findByIdAndUser(id, userId)
                .orElseThrow(() -> ApiExceptionHandler.notFound("Expense not found"));
    }

    // A missing date means "today", just like before.
    private Instant toInstant(LocalDate date) {
        return (date == null ? LocalDate.now(clock) : date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
