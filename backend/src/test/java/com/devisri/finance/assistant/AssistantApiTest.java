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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.web.servlet.ResultActions;

import com.devisri.finance.auth.User;
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

    private ResultActions send(String userId, String conversationId, String message) throws Exception {
        return mvc.perform(post("/assistant/chat").header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + message + "\",\"conversationId\":\"" + conversationId + "\"}"));
    }

    @Test
    void theAssistantRemembersTheConversationButNeverAcrossUsers() throws Exception {
        List<Prompt> prompts = new ArrayList<>();
        when(chatModel.call(any(Prompt.class))).thenAnswer(call -> {
            prompts.add(call.getArgument(0));
            return modelSays("Answer number " + prompts.size());
        });

        send("u-mem", "chat-1", "What did I spend on food?").andExpect(status().isOk());
        send("u-mem", "chat-1", "And last month?").andExpect(status().isOk());
        send("u-other", "chat-1", "Hello").andExpect(status().isOk());

        // The 2nd question is sent together with the 1st question and answer...
        assertThat(prompts.get(1).getContents()).contains("What did I spend on food?", "Answer number 1");
        // ...but another user using the same conversation id sees none of it.
        assertThat(prompts.get(2).getContents()).doesNotContain("What did I spend on food?", "Answer number");
    }

    @Test
    void aBadConversationIdIsRejected() throws Exception {
        send("u1", "../../etc", "Hi")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid conversation id"));
    }

    @Test
    void aUserIsLimitedToTenMessagesAMinute() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(modelSays("ok"));

        for (int i = 0; i < 10; i++) {
            send("u-busy", "c1", "Hi").andExpect(status().isOk());
        }
        send("u-busy", "c1", "Hi")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(ChatRateLimits.TOO_FAST));
        // Other users are not affected.
        send("u-calm", "c1", "Hi").andExpect(status().isOk());
    }

    @Test
    void demoVisitorsAreLimitedPerIpAndAFakeXForwardedForDoesNotHelp() throws Exception {
        User demo = new User("Alex Demo", "", "demo@smartfinance.app", "hashed");
        demo.setId("demo1");
        demo.setDemo(true);
        when(users.findById("demo1")).thenReturn(Optional.of(demo));
        when(chatModel.call(any(Prompt.class))).thenReturn(modelSays("ok"));

        for (int i = 0; i < 5; i++) {
            demoMessage("203.0.113.5", null).andExpect(status().isOk());
        }
        demoMessage("203.0.113.5", null).andExpect(status().isTooManyRequests());
        // Pretending to be someone else with X-Forwarded-For changes nothing.
        demoMessage("203.0.113.5", "198.51.100.1").andExpect(status().isTooManyRequests());
        // A different visitor of the same demo account can still chat.
        demoMessage("203.0.113.9", null).andExpect(status().isOk());
    }

    private ResultActions demoMessage(String cloudflareIp, String fakeForwardedFor) throws Exception {
        var request = post("/assistant/chat").header("Authorization", bearer("demo1"))
                .header("CF-Connecting-IP", cloudflareIp)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("Hi"));
        if (fakeForwardedFor != null) {
            request.header("X-Forwarded-For", fakeForwardedFor);
        }
        return mvc.perform(request);
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
