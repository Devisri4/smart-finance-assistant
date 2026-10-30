package com.devisri.finance.budget;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devisri.finance.common.ApiExceptionHandler;
import com.devisri.finance.insights.InsightsService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    public record BudgetRequest(
            @NotBlank(message = "Category is required") @Size(max = 30, message = "Category is too long") String category,
            @NotNull(message = "Limit is required") @Positive(message = "Limit must be more than 0") Double monthlyLimit) {
    }

    private final BudgetRepository budgets;
    private final InsightsService insights;

    public BudgetController(BudgetRepository budgets, InsightsService insights) {
        this.budgets = budgets;
        this.insights = insights;
    }

    /** Every budget with how much has been spent this month. */
    @GetMapping
    List<BudgetStatus> list(@AuthenticationPrincipal String userId) {
        return insights.budgetStatuses(userId);
    }

    /** Creates the budget for a category, or changes its limit if it already exists. */
    @PutMapping
    Budget save(@AuthenticationPrincipal String userId, @Valid @RequestBody BudgetRequest request) {
        String category = request.category().trim();
        Budget budget = budgets.findByUserAndCategory(userId, category)
                .orElseGet(() -> new Budget(userId, category, request.monthlyLimit()));
        budget.setMonthlyLimit(request.monthlyLimit());
        return budgets.save(budget);
    }

    @DeleteMapping("/{id}")
    Map<String, String> delete(@AuthenticationPrincipal String userId, @PathVariable String id) {
        Budget budget = budgets.findByIdAndUser(id, userId)
                .orElseThrow(() -> ApiExceptionHandler.notFound("Budget not found"));
        budgets.delete(budget);
        return Map.of("message", "Budget deleted");
    }
}
