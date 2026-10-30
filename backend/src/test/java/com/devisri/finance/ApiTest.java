package com.devisri.finance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devisri.finance.auth.User;
import com.devisri.finance.auth.UserRepository;
import com.devisri.finance.budget.BudgetRepository;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.security.JwtService;

/** Calls the real web endpoints, with a pretend database. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean UserRepository users;
    @MockitoBean ExpenseRepository expenses;
    @MockitoBean BudgetRepository budgets;

    private String bearer(String userId) {
        return "Bearer " + jwt.createToken(userId);
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("API running"));
    }

    @Test
    void expensesNeedALogin() throws Exception {
        mvc.perform(get("/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please log in"));
    }

    @Test
    void registerSavesOccupationAndHashesThePassword() throws Exception {
        when(users.existsByEmail("devi@example.com")).thenReturn(false);

        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Devi","occupation":"Developer","email":"Devi@Example.com","password":"secret1"}"""))
                .andExpect(status().isCreated());

        verify(users).save(org.mockito.ArgumentMatchers.argThat(u ->
                u.getOccupation().equals("Developer")
                        && u.getEmail().equals("devi@example.com")
                        && !u.getPassword().equals("secret1")));
    }

    @Test
    void registerRejectsAShortPassword() throws Exception {
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Devi","email":"devi@example.com","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 6 characters"));
    }

    @Test
    void registerRejectsADuplicateEmail() throws Exception {
        when(users.existsByEmail("devi@example.com")).thenReturn(true);

        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Devi","email":"devi@example.com","password":"secret1"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWorksWithTheRightPasswordOnly() throws Exception {
        User user = new User("Devi", "", "devi@example.com", passwordEncoder.encode("secret1"));
        user.setId("u1");
        when(users.findByEmail("devi@example.com")).thenReturn(Optional.of(user));

        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"devi@example.com","password":"secret1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"devi@example.com","password":"wrong-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void profileNeverShowsThePassword() throws Exception {
        User user = new User("Devi", "Developer", "devi@example.com", "hashed");
        user.setId("u1");
        when(users.findById("u1")).thenReturn(Optional.of(user));

        mvc.perform(get("/profile").header("Authorization", bearer("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Devi"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createExpenseUsesTheLoggedInUser() throws Exception {
        when(expenses.save(any(Expense.class))).thenAnswer(call -> call.getArgument(0));

        mvc.perform(post("/expenses").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Lunch","amount":250,"category":"Food","date":"2026-03-01","user":"someone-else"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lunch"))
                .andExpect(jsonPath("$.user").doesNotExist());

        verify(expenses).save(org.mockito.ArgumentMatchers.argThat(e -> e.getUser().equals("u1")));
    }

    @Test
    void createExpenseRejectsANegativeAmount() throws Exception {
        mvc.perform(post("/expenses").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Lunch","amount":-5,"category":"Food"}"""))
                .andExpect(status().isBadRequest());

        verify(expenses, never()).save(any());
    }

    @Test
    void cannotEditOrDeleteSomeoneElsesExpense() throws Exception {
        when(expenses.findByIdAndUser("e1", "u2")).thenReturn(Optional.empty());

        mvc.perform(patch("/expenses/e1").header("Authorization", bearer("u2"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Hacked","amount":1,"category":"Food"}"""))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/expenses/e1").header("Authorization", bearer("u2")))
                .andExpect(status().isNotFound());

        verify(expenses, never()).save(any());
        verify(expenses, never()).delete(any());
    }

    @Test
    void insightsAreAvailableForTheLoggedInUser() throws Exception {
        when(expenses.findByUserOrderByDateDescCreatedAtDesc("u1")).thenReturn(List.of(
                new Expense("u1", "Lunch", 250, "Food", Instant.now())));
        when(budgets.findByUserOrderByCategoryAsc("u1")).thenReturn(List.of());

        mvc.perform(get("/insights").header("Authorization", bearer("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThisMonth").value(250))
                .andExpect(jsonPath("$.topCategory").value("Food"))
                .andExpect(jsonPath("$.monthlyTrend.length()").value(6));
    }

    @Test
    void demoLoginGivesATokenWithoutAPassword() throws Exception {
        User demo = new User("Alex Demo", "", "demo@smartfinance.app", "hashed");
        demo.setId("demo1");
        when(users.findByEmail("demo@smartfinance.app")).thenReturn(Optional.of(demo));

        mvc.perform(post("/demo-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        verify(expenses).deleteByUser("demo1");
        verify(expenses).saveAll(any());
    }
}
