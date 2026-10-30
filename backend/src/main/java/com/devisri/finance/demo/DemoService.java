package com.devisri.finance.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devisri.finance.auth.User;
import com.devisri.finance.auth.UserRepository;
import com.devisri.finance.budget.Budget;
import com.devisri.finance.budget.BudgetRepository;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;

/** Prepares the shared demo account that the "Try demo" button logs in to. */
@Service
public class DemoService {

    public static final String DEMO_EMAIL = "demo@smartfinance.app";

    /** title, category, smallest amount, largest amount, how many times a month */
    private record Template(String title, String category, int min, int max, int perMonth) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("Groceries", "Food", 600, 1800, 4),
            new Template("Restaurant dinner", "Food", 400, 1500, 2),
            new Template("Coffee", "Food", 120, 300, 4),
            new Template("Metro card top-up", "Transport", 300, 600, 2),
            new Template("Cab ride", "Transport", 150, 500, 3),
            new Template("Electricity bill", "Bills", 1200, 2200, 1),
            new Template("Mobile & internet", "Bills", 600, 900, 1),
            new Template("Clothes", "Shopping", 800, 3000, 1),
            new Template("Online order", "Shopping", 300, 1500, 2),
            new Template("Movie tickets", "Entertainment", 300, 900, 1),
            new Template("Streaming subscription", "Entertainment", 199, 649, 1),
            new Template("Pharmacy", "Other", 150, 800, 1));

    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoService(UserRepository users, ExpenseRepository expenses, BudgetRepository budgets,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.expenses = expenses;
        this.budgets = budgets;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Wipes the demo account and fills it with fresh example data around today's date. */
    public User resetDemoUser() {
        User demo = users.findByEmail(DEMO_EMAIL).orElseGet(() -> {
            // Random password that nobody knows: the demo account can only be opened with the demo button.
            User user = new User("Alex Demo", "Software Engineer", DEMO_EMAIL,
                    passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setDemo(true);
            return users.save(user);
        });

        expenses.deleteByUser(demo.getId());
        budgets.deleteByUser(demo.getId());
        expenses.saveAll(sampleExpenses(demo.getId(), LocalDate.now(clock)));
        budgets.saveAll(List.of(
                new Budget(demo.getId(), "Food", 9000),
                new Budget(demo.getId(), "Shopping", 2500),
                new Budget(demo.getId(), "Entertainment", 1500),
                new Budget(demo.getId(), "Transport", 3000)));
        return demo;
    }

    static List<Expense> sampleExpenses(String userId, LocalDate today) {
        Random random = new Random(42); // same "random" data every time
        List<Expense> list = new ArrayList<>();
        for (int monthsAgo = 3; monthsAgo >= 0; monthsAgo--) {
            LocalDate first = today.minusMonths(monthsAgo).withDayOfMonth(1);
            int lastDay = monthsAgo == 0 ? today.getDayOfMonth() : first.lengthOfMonth();
            list.add(expense(userId, "Rent", 15000, "Bills", first));
            for (Template t : TEMPLATES) {
                for (int i = 0; i < t.perMonth(); i++) {
                    LocalDate day = first.withDayOfMonth(1 + random.nextInt(first.lengthOfMonth()));
                    double amount = t.min() + random.nextInt(t.max() - t.min() + 1);
                    if (day.getDayOfMonth() <= lastDay) {
                        list.add(expense(userId, t.title(), amount, t.category(), day));
                    }
                }
            }
        }
        return list;
    }

    private static Expense expense(String userId, String title, double amount, String category, LocalDate day) {
        return new Expense(userId, title, amount, category, day.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
