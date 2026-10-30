package com.devisri.finance.insights;

import java.util.List;

import com.devisri.finance.budget.BudgetStatus;

/** Everything the "Insights" part of the dashboard shows, worked out on the server. */
public record InsightsResponse(
        String month,
        double totalThisMonth,
        double totalLastMonth,
        /** Compared with the same day of last month. Null when there is nothing to compare with. */
        Integer changePercent,
        double dailyAverage,
        double projectedMonthTotal,
        String topCategory,
        List<CategorySummary> categories,
        List<MonthTotal> monthlyTrend,
        List<BudgetStatus> budgets,
        List<Alert> alerts,
        List<String> tips) {

    public record CategorySummary(String category, double thisMonth, double lastMonth) {
    }

    public record MonthTotal(String month, double total) {
    }

    public record Alert(Level level, String message) {
        public enum Level { WARNING, DANGER }
    }
}
