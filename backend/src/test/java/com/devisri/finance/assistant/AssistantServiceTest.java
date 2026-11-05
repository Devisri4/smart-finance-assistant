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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.server.ResponseStatusException;

class AssistantServiceTest {

    @Test
    void withoutAnApiKeyTheAssistantSaysItIsNotSetUpAndNeverCallsTheModel() {
        ChatModel model = mock(ChatModel.class);
        AssistantService service = new AssistantService(ChatClient.builder(model), mock(FinanceTools.class),
                Clock.systemUTC(), AssistantService.NOT_CONFIGURED);

        assertThatThrownBy(() -> service.chat("u1", "Hi"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("isn't set up yet");
        verify(model, never()).call(any(Prompt.class));
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
