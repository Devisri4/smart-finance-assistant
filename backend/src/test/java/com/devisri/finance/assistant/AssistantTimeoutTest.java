package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devisri.finance.auth.UserRepository;
import com.devisri.finance.budget.BudgetRepository;
import com.devisri.finance.expense.ExpenseRepository;
import com.devisri.finance.security.JwtService;

/**
 * Uses the REAL OpenAI-compatible client against a fake AI server that accepts the connection
 * but never answers. Proves the read timeout really works and the user gets a friendly message.
 */
@SpringBootTest(properties = {
        "spring.http.client.read-timeout=1s",
        "spring.ai.retry.max-attempts=1"
})
@AutoConfigureMockMvc
class AssistantTimeoutTest {

    private static final ServerSocket SILENT_SERVER;
    private static final List<Socket> OPEN = new CopyOnWriteArrayList<>();

    static {
        try {
            SILENT_SERVER = new ServerSocket(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Thread acceptor = new Thread(() -> {
            while (!SILENT_SERVER.isClosed()) {
                try {
                    OPEN.add(SILENT_SERVER.accept()); // accept, then say nothing
                } catch (IOException e) {
                    return;
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @DynamicPropertySource
    static void aiServer(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", () -> "http://localhost:" + SILENT_SERVER.getLocalPort());
    }

    @AfterAll
    static void stop() throws IOException {
        for (Socket socket : OPEN) {
            socket.close();
        }
        SILENT_SERVER.close();
    }

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @MockitoBean UserRepository users;
    @MockitoBean ExpenseRepository expenses;
    @MockitoBean BudgetRepository budgets;

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void aSilentAiProviderTimesOutWithAFriendlyMessage() throws Exception {
        long start = System.currentTimeMillis();

        mvc.perform(post("/assistant/chat").header("Authorization", "Bearer " + jwt.createToken("u1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(AssistantService.PROVIDER_ERROR_MESSAGE));

        assertThat(System.currentTimeMillis() - start).isLessThan(10_000);
    }
}
