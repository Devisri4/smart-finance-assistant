package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import com.devisri.finance.assistant.FinanceTools.ExpenseList;
import com.devisri.finance.budget.BudgetStatus;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.insights.BudgetSuggestion;
import com.devisri.finance.insights.InsightsResponse;
import com.devisri.finance.insights.InsightsResponse.CategorySummary;
import com.devisri.finance.insights.InsightsService;

@ExtendWith(MockitoExtension.class)
class FinanceToolsTest {

    @Mock InsightsService insights;
    @Mock ExpenseRepository expenses;
    @InjectMocks FinanceTools tools;

    private final ToolResults results = new ToolResults();

    private ToolContext contextFor(String userId) {
        return new ToolContext(Map.of(FinanceTools.USER_ID, userId, FinanceTools.RESULTS, results));
    }

    private static InsightsResponse summaryWith(List<CategorySummary> categories) {
        return new InsightsResponse("2026-03", 3000, 7000, 50, 200, 6200, "Food", categories,
                List.of(), List.of(), List.of(), List.of());
    }

    private static Expense expense(String title, double amount, String category, LocalDate day) {
        return new Expense("u1", title, amount, category, day.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    void spendingSummaryUsesTheLoggedInUserAndRecordsTheRealChartData() {
        when(insights.insights("u1")).thenReturn(summaryWith(List.of(
                new CategorySummary("Food", 2000, 1500),
                new CategorySummary("Shopping", 0, 900))));

        InsightsResponse result = tools.getSpendingSummary(contextFor("u1"));

        assertThat(result.totalThisMonth()).isEqualTo(3000);
        verify(insights).insights("u1");
        // The chart comes from the tool result and leaves out categories with nothing spent this month.
        assertThat(results.chart().type()).isEqualTo("category-donut");
        assertThat(results.chart().data()).extracting(CategorySummary::category).containsExactly("Food");
        assertThat(results.toolsUsed()).containsExactly("getSpendingSummary");
    }

    @Test
    void budgetStatusUsesTheLoggedInUser() {
        BudgetStatus food = new BudgetStatus("b1", "Food", 5000, 4500, 500, 90, BudgetStatus.Level.WARNING);
        when(insights.budgetStatuses("u1")).thenReturn(List.of(food));

        assertThat(tools.getBudgetStatus(contextFor("u1"))).containsExactly(food);
        assertThat(results.chart()).isNull();
    }

    @Test
    void budgetSuggestionsComeFromTheJavaCalculation() {
        BudgetSuggestion food = new BudgetSuggestion("Food", 3525, 3600, 3000.0, 2);
        when(insights.budgetSuggestions("u1")).thenReturn(List.of(food));

        assertThat(tools.suggestBudgets(contextFor("u1"))).containsExactly(food);
        assertThat(results.toolsUsed()).containsExactly("suggestBudgets");
    }

    @Test
    void expensesAreFilteredByMonthAndCategoryAndCountTheFullTotal() {
        when(expenses.findByUserOrderByDateDescCreatedAtDesc("u1")).thenReturn(List.of(
                expense("Dinner", 800, "Food", LocalDate.of(2026, 3, 10)),
                expense("Groceries", 1200, "Food", LocalDate.of(2026, 3, 2)),
                expense("Cab", 300, "Transport", LocalDate.of(2026, 3, 5)),
                expense("Old groceries", 999, "Food", LocalDate.of(2026, 2, 20))));

        ExpenseList result = tools.getExpenses("2026-03", "food", 1, contextFor("u1"));

        assertThat(result.matchingCount()).isEqualTo(2);
        assertThat(result.matchingTotal()).isEqualTo(2000);
        assertThat(result.expenses()).singleElement().satisfies(e -> {
            assertThat(e.title()).isEqualTo("Dinner");
            assertThat(e.date()).isEqualTo("2026-03-10");
        });
    }

    @Test
    void expensesAreCappedAtTwentyEvenIfTheModelAsksForMore() {
        List<Expense> many = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            many.add(expense("Coffee " + i, 100, "Food", LocalDate.of(2026, 3, 1)));
        }
        when(expenses.findByUserOrderByDateDescCreatedAtDesc("u1")).thenReturn(many);

        ExpenseList result = tools.getExpenses(null, null, 500, contextFor("u1"));

        assertThat(result.expenses()).hasSize(FinanceTools.MAX_EXPENSES);
        assertThat(result.matchingCount()).isEqualTo(30);
    }

    @Test
    void aBadMonthIsRejectedWithAClearMessageBeforeReadingAnything() {
        assertThatThrownBy(() -> tools.getExpenses("March", null, null, contextFor("u1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2026-03");
        verifyNoInteractions(expenses);
    }

    @Test
    void withoutALoggedInUserNoDataIsRead() {
        ToolContext noUser = new ToolContext(Map.of());

        assertThatThrownBy(() -> tools.getSpendingSummary(noUser)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tools.getBudgetStatus(noUser)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tools.suggestBudgets(noUser)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tools.getExpenses(null, null, null, noUser))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(insights, expenses);
    }

    @Test
    void theModelCanNeverChooseWhichUserToRead() {
        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        assertThat(callbacks).extracting(c -> c.getToolDefinition().name())
                .containsExactlyInAnyOrder("getSpendingSummary", "getBudgetStatus", "suggestBudgets", "getExpenses");
        // The JSON schema the model sees has no user field, and no ToolContext either.
        for (ToolCallback callback : callbacks) {
            assertThat(callback.getToolDefinition().inputSchema().toLowerCase())
                    .doesNotContain("user")
                    .doesNotContain("context");
        }
    }

    @Test
    void toolsOnlyReadExpensesAndNeverSaveOrDelete() {
        when(insights.insights(anyString())).thenReturn(summaryWith(List.of()));
        when(insights.budgetStatuses(anyString())).thenReturn(List.of());
        when(insights.budgetSuggestions(anyString())).thenReturn(List.of());
        when(expenses.findByUserOrderByDateDescCreatedAtDesc(anyString())).thenReturn(List.of());

        tools.getSpendingSummary(contextFor("u1"));
        tools.getBudgetStatus(contextFor("u1"));
        tools.suggestBudgets(contextFor("u1"));
        tools.getExpenses(null, null, null, contextFor("u1"));

        verify(expenses).findByUserOrderByDateDescCreatedAtDesc("u1");
        org.mockito.Mockito.verifyNoMoreInteractions(expenses);
    }
}
