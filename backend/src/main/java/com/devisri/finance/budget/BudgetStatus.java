package com.devisri.finance.budget;

/** A budget plus how much of it has been used this month. */
public record BudgetStatus(String id, String category, double monthlyLimit, double spent, double remaining,
                           int percentUsed, Level level) {

    /** At 80% we warn, above 100% the budget is over. */
    public static final int WARNING_PERCENT = 80;

    public enum Level { OK, WARNING, OVER }

    public static BudgetStatus of(Budget budget, double spent) {
        double limit = budget.getMonthlyLimit();
        int percent = limit <= 0 ? 0 : (int) Math.round(spent * 100 / limit);
        Level level = spent > limit ? Level.OVER : percent >= WARNING_PERCENT ? Level.WARNING : Level.OK;
        return new BudgetStatus(budget.getId(), budget.getCategory(), limit, spent, limit - spent, percent, level);
    }
}
