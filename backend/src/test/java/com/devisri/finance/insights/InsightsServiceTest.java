package com.devisri.finance.insights;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devisri.finance.budget.Budget;
import com.devisri.finance.budget.BudgetStatus;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.insights.InsightsResponse.Alert;

class InsightsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    private static Expense expense(String title, double amount, String category, LocalDate day) {
        return new Expense("u1", title, amount, category, day.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    void comparesThisMonthWithTheSamePointLastMonth() {
        List<Expense> expenses = List.of(
                expense("Groceries", 1500, "Food", LocalDate.of(2026, 3, 2)),
                expense("Shoes", 1500, "Shopping", LocalDate.of(2026, 3, 10)),
                expense("Groceries", 2000, "Food", LocalDate.of(2026, 2, 5)),
                expense("Late Feb dinner", 5000, "Food", LocalDate.of(2026, 2, 25)));

        InsightsResponse result = InsightsService.build(expenses, List.of(), TODAY);

        assertThat(result.month()).isEqualTo("2026-03");
        assertThat(result.totalThisMonth()).isEqualTo(3000);
        assertThat(result.totalLastMonth()).isEqualTo(7000);
        // 3000 now vs 2000 by 15 Feb = 50% more
        assertThat(result.changePercent()).isEqualTo(50);
        assertThat(result.topCategory()).isIn("Food", "Shopping");
        assertThat(result.dailyAverage()).isEqualTo(200);
        assertThat(result.projectedMonthTotal()).isEqualTo(6200);
        assertThat(result.tips()).anyMatch(t -> t.contains("50% more"));
    }

    @Test
    void warnsWhenABudgetIsNearlyUsedAndWhenItIsOver() {
        List<Expense> expenses = List.of(
                expense("Groceries", 900, "Food", LocalDate.of(2026, 3, 3)),
                expense("Jacket", 3000, "Shopping", LocalDate.of(2026, 3, 4)));
        List<Budget> budgets = List.of(new Budget("u1", "Food", 1000), new Budget("u1", "Shopping", 2000),
                new Budget("u1", "Transport", 1000));

        InsightsResponse result = InsightsService.build(expenses, budgets, TODAY);

        assertThat(result.budgets()).extracting(BudgetStatus::level)
                .containsExactly(BudgetStatus.Level.WARNING, BudgetStatus.Level.OVER, BudgetStatus.Level.OK);
        assertThat(result.alerts()).extracting(Alert::level)
                .containsExactly(Alert.Level.WARNING, Alert.Level.DANGER);
        assertThat(result.alerts().get(1).message()).contains("₹1,000 over your Shopping budget");
    }

    @Test
    void buildsASixMonthTrendEndingThisMonth() {
        List<Expense> expenses = List.of(expense("Rent", 10000, "Bills", LocalDate.of(2025, 12, 1)));

        InsightsResponse result = InsightsService.build(expenses, List.of(), TODAY);

        assertThat(result.monthlyTrend()).hasSize(6);
        assertThat(result.monthlyTrend().get(0).month()).isEqualTo("2025-10");
        assertThat(result.monthlyTrend().get(5).month()).isEqualTo("2026-03");
        assertThat(result.monthlyTrend().get(2).total()).isEqualTo(10000);
    }

    @Test
    void emptyMonthGivesAFriendlyTip() {
        InsightsResponse result = InsightsService.build(List.of(), List.of(), TODAY);

        assertThat(result.totalThisMonth()).isZero();
        assertThat(result.changePercent()).isNull();
        assertThat(result.tips()).singleElement().asString().contains("No spending recorded yet");
    }
}
