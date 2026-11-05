package com.devisri.finance.assistant;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.devisri.finance.insights.InsightsResponse.CategorySummary;

/**
 * Notes what the tools did during ONE chat request: which tools ran, and the real category
 * totals for the chart. The chart is built from here (real data from Java code),
 * never from numbers the AI model wrote.
 */
public class ToolResults {

    private final Set<String> toolsUsed = new LinkedHashSet<>();
    private Chart chart;

    public synchronized void toolUsed(String name) {
        toolsUsed.add(name);
    }

    public synchronized void recordCategoryChart(String month, List<CategorySummary> categories) {
        List<CategorySummary> nonZero = categories.stream().filter(c -> c.thisMonth() > 0).toList();
        chart = nonZero.isEmpty() ? null : new Chart(Chart.CATEGORY_DONUT, month, nonZero);
    }

    public synchronized Set<String> toolsUsed() {
        return Set.copyOf(toolsUsed);
    }

    public synchronized Chart chart() {
        return chart;
    }

    /** Data for a chart in the chat. "data" has the same shape the React CategoryDonut already uses. */
    public record Chart(String type, String month, List<CategorySummary> data) {
        public static final String CATEGORY_DONUT = "category-donut";
    }
}
