package com.devisri.finance.insights;

/**
 * A suggested monthly limit for one category, worked out from past spending.
 *
 * @param currentLimit the budget the user already has for this category, or null if none
 * @param monthsOfData how many past months the average is based on (1 to 3)
 */
public record BudgetSuggestion(String category, double averageMonthlySpend, double suggestedLimit,
                               Double currentLimit, int monthsOfData) {
}
