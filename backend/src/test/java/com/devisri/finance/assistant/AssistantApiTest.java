package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devisri.finance.auth.UserRepository;
import com.devisri.finance.budget.BudgetRepository;
import com.devisri.finance.expense.Expense;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.security.JwtService;

/** Calls the real chat endpoint. The AI model and the database are pretend; the tools are real. */
@SpringBootTest
@AutoConfigureMockMvc
class AssistantApiTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @MockitoBean ChatModel chatModel;
    @MockitoBean UserRepository users;
    @MockitoBean ExpenseRepository expenses;
    @MockitoBean BudgetRepository budgets;

    private String bearer(String userId) {
        return "Bearer " + jwt.createToken(userId);
    }

    private static String chatBody(String message) {
        return "{\"message\":\"" + message + "\"}";
    }

    private static ChatResponse modelSays(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void chatNeedsALogin() throws Exception {
        mvc.perform(post("/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(chatBody("Hi")))
                .andExpect(status().isUnauthorized());

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void toolsReadTheLoggedInUsersDataAndTheChartUsesRealNumbers() throws Exception {
        when(expenses.findByUserOrderByDateDescCreatedAtDesc("u1")).thenReturn(List.of(
                new Expense("u1", "Groceries", 1200, "Food", Instant.now()),
                new Expense("u1", "Cab", 300, "Transport", Instant.now())));
        when(budgets.findByUserOrderByCategoryAsc("u1")).thenReturn(List.of());

        AtomicReference<Prompt> sentPrompt = new AtomicReference<>();
        // Act like a real model that decides to call the getSpendingSummary tool.
        when(chatModel.call(any(Prompt.class))).thenAnswer(call -> {
            Prompt prompt = call.getArgument(0);
            sentPrompt.set(prompt);
            ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
            ToolCallback summaryTool = options.getToolCallbacks().stream()
                    .filter(t -> t.getToolDefinition().name().equals("getSpendingSummary"))
                    .findFirst().orElseThrow();
            summaryTool.call("{}", new ToolContext(options.getToolContext()));
            return modelSays("You spent ₹1,500 this month. Food is the biggest category.");
        });

        mvc.perform(post("/assistant/chat").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("Show my spending breakdown this month")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("You spent ₹1,500 this month. Food is the biggest category."))
                .andExpect(jsonPath("$.chart.type").value("category-donut"))
                .andExpect(jsonPath("$.chart.data[0].category").value("Food"))
                .andExpect(jsonPath("$.chart.data[0].thisMonth").value(1200))
                .andExpect(jsonPath("$.chart.data[1].thisMonth").value(300))
                .andExpect(jsonPath("$.suggestions.length()").value(3));

        // The tool read user u1's data: the user from the token.
        verify(expenses).findByUserOrderByDateDescCreatedAtDesc("u1");
        // The user id is in the tool context only, never in the text sent to the model.
        assertThat(sentPrompt.get().getContents()).doesNotContain("u1");
        assertThat(sentPrompt.get().getSystemMessage().getText()).contains("Never guess");
    }

    @Test
    void aTooLongMessageIsRejectedBeforeCallingTheModel() throws Exception {
        mvc.perform(post("/assistant/chat").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content(chatBody("a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Message is too long (max 500 characters)"));

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void anEmptyMessageIsRejected() throws Exception {
        mvc.perform(post("/assistant/chat").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content(chatBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please type a message"));
    }

    @Test
    void anAiProviderFailureGivesAFriendlyMessage() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("connection reset by provider"));

        mvc.perform(post("/assistant/chat").header("Authorization", bearer("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content(chatBody("Suggest a budget")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(AssistantService.PROVIDER_ERROR_MESSAGE));
    }
}
