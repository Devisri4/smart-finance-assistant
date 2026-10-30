package com.devisri.finance.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devisri.finance.expense.Expense;

class DemoServiceTest {

    @Test
    void sampleDataCoversFourMonthsAndNeverLiesInTheFuture() {
        LocalDate today = LocalDate.of(2026, 3, 15);

        List<Expense> sample = DemoService.sampleExpenses("demo", today);

        assertThat(sample).isNotEmpty();
        assertThat(sample).allSatisfy(e -> {
            LocalDate day = e.getDate().atZone(ZoneOffset.UTC).toLocalDate();
            assertThat(day).isBetween(LocalDate.of(2025, 12, 1), today);
            assertThat(e.getAmount()).isPositive();
        });
        assertThat(sample).filteredOn(e -> e.getTitle().equals("Rent")).hasSize(4);
    }
}
