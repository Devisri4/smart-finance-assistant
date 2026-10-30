package com.devisri.finance.insights;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.devisri.finance.budget.Budget;
import com.devisri.finance.budget.BudgetRepository;
import com.devisri.finance.budget.BudgetStatus;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.insights.InsightsResponse.Alert;
import com.devisri.finance.insights.InsightsResponse.CategorySummary;
import com.devisri.finance.insights.InsightsResponse.MonthTotal;

/** Looks at a user's expenses and budgets and explains them in plain sentences. */
@Service
public class InsightsService {

    private static final int TREND_MONTHS = 6;

    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final Clock clock;

    public InsightsService(ExpenseRepository expenses, BudgetRepository budgets, Clock clock) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.clock = clock;
    }

    public InsightsResponse insights(String userId) {
        return build(expenses.findByUserOrderByDateDescCreatedAtDesc(userId),
                budgets.findByUserOrderByCategoryAsc(userId), LocalDate.now(clock));
    }

    public List<BudgetStatus> budgetStatuses(String userId) {
        List<Expense> all = expenses.findByUserOrderByDateDescCreatedAtDesc(userId);
        return budgetStatuses(budgets.findByUserOrderByCategoryAsc(userId),
                byCategory(inMonth(all, YearMonth.from(LocalDate.now(clock)))));
    }

    /** The real work. It has no database code, so tests can call it directly. */
    static InsightsResponse build(List<Expense> all, List<Budget> userBudgets, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        int dayOfMonth = today.getDayOfMonth();

        List<Expense> current = inMonth(all, thisMonth);
        List<Expense> previous = inMonth(all, lastMonth);
        double totalThis = sum(current);
        double totalLast = sum(previous);
        // Compare like with like: the 1st to today's date in both months.
        double lastToDate = sum(filter(previous, e -> day(e).getDayOfMonth() <= dayOfMonth));
        Integer changePercent = lastToDate > 0 ? (int) Math.round((totalThis - lastToDate) * 100 / lastToDate) : null;
        double dailyAverage = totalThis / dayOfMonth;
        double projected = dailyAverage * thisMonth.lengthOfMonth();

        Map<String, Double> thisByCategory = byCategory(current);
        Map<String, Double> lastByCategory = byCategory(previous);
        List<String> categoryNames = new ArrayList<>(thisByCategory.keySet());
        lastByCategory.keySet().stream().filter(c -> !thisByCategory.containsKey(c)).forEach(categoryNames::add);
        List<CategorySummary> categories = categoryNames.stream()
                .map(c -> new CategorySummary(c, round(thisByCategory.getOrDefault(c, 0.0)),
                        round(lastByCategory.getOrDefault(c, 0.0))))
                .sorted(Comparator.comparingDouble(CategorySummary::thisMonth).reversed())
                .toList();
        String topCategory = thisByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        List<MonthTotal> trend = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = thisMonth.minusMonths(i);
            trend.add(new MonthTotal(month.toString(), round(sum(inMonth(all, month)))));
        }

        List<BudgetStatus> statuses = budgetStatuses(userBudgets, thisByCategory);
        List<Alert> alerts = new ArrayList<>();
        for (BudgetStatus status : statuses) {
            if (status.level() == BudgetStatus.Level.OVER) {
                alerts.add(new Alert(Alert.Level.DANGER, "You are %s over your %s budget."
                        .formatted(money(-status.remaining()), status.category())));
            } else if (status.level() == BudgetStatus.Level.WARNING) {
                alerts.add(new Alert(Alert.Level.WARNING, "You have used %d%% of your %s budget (%s left)."
                        .formatted(status.percentUsed(), status.category(), money(status.remaining()))));
            }
        }

        List<String> tips = new ArrayList<>();
        if (current.isEmpty()) {
            tips.add("No spending recorded yet this month. Add an expense to see insights.");
        } else {
            if (changePercent != null && changePercent != 0) {
                tips.add("So far this month you have spent %d%% %s than at this point last month."
                        .formatted(Math.abs(changePercent), changePercent > 0 ? "more" : "less"));
            }
            if (topCategory != null && totalThis > 0) {
                double topAmount = thisByCategory.get(topCategory);
                tips.add("%s is your biggest expense this month: %s (%d%% of your spending)."
                        .formatted(topCategory, money(topAmount), Math.round(topAmount * 100 / totalThis)));
            }
            biggestIncrease(thisByCategory, lastByCategory).ifPresent(tips::add);
            if (dayOfMonth >= 5 && dayOfMonth < thisMonth.lengthOfMonth()) {
                tips.add("At this pace you will spend about %s by the end of the month.".formatted(money(projected)));
            }
            current.stream().max(Comparator.comparingDouble(Expense::getAmount)).ifPresent(e ->
                    tips.add("Your largest single expense this month was \"%s\" (%s)."
                            .formatted(e.getTitle(), money(e.getAmount()))));
        }

        return new InsightsResponse(thisMonth.toString(), round(totalThis), round(totalLast), changePercent,
                round(dailyAverage), round(projected), topCategory, categories, trend, statuses, alerts, tips);
    }

    private static List<BudgetStatus> budgetStatuses(List<Budget> userBudgets, Map<String, Double> spentByCategory) {
        return userBudgets.stream()
                .map(b -> BudgetStatus.of(b, round(spentByCategory.getOrDefault(b.getCategory(), 0.0))))
                .toList();
    }

    private static Optional<String> biggestIncrease(Map<String, Double> now, Map<String, Double> before) {
        return now.entrySet().stream()
                .filter(e -> before.getOrDefault(e.getKey(), 0.0) > 0)
                .map(e -> Map.entry(e.getKey(),
                        (e.getValue() - before.get(e.getKey())) * 100 / before.get(e.getKey())))
                .filter(e -> e.getValue() >= 20)
                .max(Map.Entry.comparingByValue())
                .map(e -> "%s is up %d%% compared with the whole of last month."
                        .formatted(e.getKey(), Math.round(e.getValue())));
    }

    private static List<Expense> inMonth(List<Expense> all, YearMonth month) {
        return filter(all, e -> e.getDate() != null && YearMonth.from(day(e)).equals(month));
    }

    private static List<Expense> filter(List<Expense> list, Predicate<Expense> test) {
        return list.stream().filter(test).toList();
    }

    private static Map<String, Double> byCategory(List<Expense> list) {
        Map<String, Double> totals = new LinkedHashMap<>();
        list.forEach(e -> totals.merge(Objects.requireNonNullElse(e.getCategory(), "Other"), e.getAmount(),
                Double::sum));
        return totals;
    }

    private static LocalDate day(Expense e) {
        return e.getDate().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static double sum(List<Expense> list) {
        return list.stream().mapToDouble(Expense::getAmount).sum();
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static String money(double value) {
        return "₹" + String.format(Locale.ENGLISH, "%,.0f", value);
    }
}
