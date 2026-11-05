package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.server.ResponseStatusException;

class AssistantServiceTest {

    @Test
    void withoutAnApiKeyTheAssistantSaysItIsNotSetUpAndNeverCallsTheModel() {
        ChatModel model = mock(ChatModel.class);
        AssistantService service = new AssistantService(ChatClient.builder(model), mock(FinanceTools.class),
                MessageWindowChatMemory.builder().build(), Clock.systemUTC(), AssistantService.NOT_CONFIGURED);

        assertThatThrownBy(() -> service.chat("u1", null, "Hi"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("isn't set up yet");
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void memoryIsKeptPerUserAndPerConversation() {
        assertThat(AssistantService.memoryKey("u1", "abc")).isEqualTo("u1:abc");
        assertThat(AssistantService.memoryKey("u1", null)).isEqualTo("u1:default");
        assertThat(AssistantService.memoryKey("u2", "abc")).isNotEqualTo(AssistantService.memoryKey("u1", "abc"));
    }

    @Test
    void followUpChipsSkipWhatWasJustAnswered() {
        assertThat(AssistantService.followUps(Set.of()))
                .containsExactly("Show my spending breakdown this month", "How are my budgets doing?",
                        "Suggest a budget");
        assertThat(AssistantService.followUps(Set.of("getSpendingSummary")))
                .containsExactly("How are my budgets doing?", "Suggest a budget", "Where can I save money?");
    }
}
