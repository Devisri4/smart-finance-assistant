package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

class BoundedChatMemoryRepositoryTest {

    @Test
    void keepsOnlyTheMostRecentlyUsedConversations() {
        BoundedChatMemoryRepository repository = new BoundedChatMemoryRepository(2);
        repository.saveAll("u1:a", List.of(new UserMessage("first")));
        repository.saveAll("u1:b", List.of(new UserMessage("second")));
        repository.findByConversationId("u1:a"); // "a" is now the most recently used
        repository.saveAll("u2:c", List.of(new UserMessage("third")));

        assertThat(repository.findConversationIds()).containsExactlyInAnyOrder("u1:a", "u2:c");
        assertThat(repository.findByConversationId("u1:b")).isEmpty();
    }

    @Test
    void canDeleteAConversation() {
        BoundedChatMemoryRepository repository = new BoundedChatMemoryRepository(10);
        repository.saveAll("u1:a", List.of(new UserMessage("hi")));
        repository.deleteByConversationId("u1:a");

        assertThat(repository.findByConversationId("u1:a")).isEmpty();
    }
}
