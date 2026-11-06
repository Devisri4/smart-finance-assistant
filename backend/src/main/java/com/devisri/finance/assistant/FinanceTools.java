package com.devisri.finance.assistant;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.devisri.finance.budget.BudgetStatus;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.insights.BudgetSuggestion;
import com.devisri.finance.insights.InsightsResponse;
import com.devisri.finance.insights.InsightsService;

/**
 * The read-only "tools" the AI model may call. Each one only wraps code the app already has.
 *
 * Security: no tool takes a user id as input, so the model can never pick whose data it sees.
 * The user id comes from the login token: the controller puts it in the ToolContext, which
 * Spring AI hands to the tool directly and never sends to the model.
 */
@Component
public class FinanceTools {

    static final String USER_ID = "userId";
    static final String RESULTS = "toolResults";
    static final int MAX_EXPENSES = 20;
    private static final int DEFAULT_EXPENSES = 10;

    private final InsightsService insights;
    private final ExpenseRepository expenses;

    public FinanceTools(InsightsService insights, ExpenseRepository expenses) {
        this.insights = insights;
        this.expenses = expenses;
    }

    @Tool(description = """
            Get the user's spending summary for the current month: total spent, total last month, \
            percent change compared with the same day last month, daily average, projected month-end total, \
            top category, spending per category (this month and last month), 6-month trend, \
            budget alerts and tips. Use it for questions about spending, breakdowns, trends and saving money.""")
    public InsightsResponse getSpendingSummary(ToolContext context) {
        InsightsResponse summary = insights.insights(userId(context));
        ToolResults results = results(context);
        results.toolUsed("getSpendingSummary");
        results.recordCategoryChart(summary.month(), summary.categories());
        return summary;
    }

    @Tool(description = """
            Get every monthly budget the user has set, with the amount spent this month, the amount \
            remaining (negative means over budget), percent used and level: OK, WARNING (80% or more used) \
            or OVER.""")
    public List<BudgetStatus> getBudgetStatus(ToolContext context) {
        List<BudgetStatus> statuses = insights.budgetStatuses(userId(context));
        results(context).toolUsed("getBudgetStatus");
        return statuses;
    }

    @Tool(description = """
            Suggest a monthly budget for each category. It is calculated in code from the average the user \
            spent in up to the last 3 full months, rounded up to the next 100, and also shows the current \
            limit if one is set. Always use this for budget suggestions; never calculate budgets yourself. \
            An empty list means there is not enough past spending yet.""")
    public List<BudgetSuggestion> suggestBudgets(ToolContext context) {
        List<BudgetSuggestion> suggestions = insights.budgetSuggestions(userId(context));
        results(context).toolUsed("suggestBudgets");
        return suggestions;
    }

    @Tool(description = """
            List the user's individual expenses, newest first, optionally filtered by month and category. \
            Returns how many expenses match and their total, plus at most 20 of them.""")
    public ExpenseList getExpenses(
            @ToolParam(description = "Month as YYYY-MM, for example 2026-03. Leave empty for all months.",
                    required = false) String month,
            @ToolParam(description = "One of: Food, Transport, Bills, Shopping, Entertainment, Other. "
                    + "Leave empty for all categories.", required = false) String category,
            @ToolParam(description = "How many expenses to return, 1 to 20. Default 10.",
                    required = false) Integer limit,
            ToolContext context) {
        String userId = userId(context);
        YearMonth wantedMonth = parseMonth(month);
        String wantedCategory = isBlank(category) ? null : category.trim();
        int max = limit == null ? DEFAULT_EXPENSES : Math.max(1, Math.min(limit, MAX_EXPENSES));

        List<Expense> matching = expenses.findByUserOrderByDateDescCreatedAtDesc(userId).stream()
                .filter(e -> wantedMonth == null
                        || (e.getDate() != null && YearMonth.from(e.getDate().atZone(ZoneOffset.UTC)).equals(wantedMonth)))
                .filter(e -> wantedCategory == null || wantedCategory.equalsIgnoreCase(e.getCategory()))
                .toList();
        double total = Math.round(matching.stream().mapToDouble(Expense::getAmount).sum() * 100) / 100.0;
        List<ExpenseItem> items = matching.stream().limit(max).map(ExpenseItem::from).toList();

        results(context).toolUsed("getExpenses");
        return new ExpenseList(wantedMonth == null ? null : wantedMonth.toString(), wantedCategory,
                matching.size(), total, items);
    }

    public record ExpenseList(String month, String category, int matchingCount, double matchingTotal,
                              List<ExpenseItem> expenses) {
    }

    public record ExpenseItem(String title, double amount, String category, String date) {
        static ExpenseItem from(Expense e) {
            String day = e.getDate() == null ? null : e.getDate().atZone(ZoneOffset.UTC).toLocalDate().toString();
            return new ExpenseItem(e.getTitle(), e.getAmount(), e.getCategory(), day);
        }
    }

    /** Fails closed: without a logged-in user, no tool returns any data. */
    static String userId(ToolContext context) {
        Object id = context == null ? null : context.getContext().get(USER_ID);
        if (!(id instanceof String userId) || userId.isBlank()) {
            throw new IllegalStateException("No logged-in user for this tool call");
        }
        return userId;
    }

    private static ToolResults results(ToolContext context) {
        return context.getContext().get(RESULTS) instanceof ToolResults r ? r : new ToolResults();
    }

    private static YearMonth parseMonth(String month) {
        if (isBlank(month)) {
            return null;
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Month must look like 2026-03");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
