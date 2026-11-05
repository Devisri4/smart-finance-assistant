package com.devisri.finance.assistant;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AssistantConfig {

    /** How many recent messages (questions + answers) the assistant remembers per conversation. */
    static final int MEMORY_MESSAGES = 10;

    @Bean
    ChatMemory chatMemory(BoundedChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(MEMORY_MESSAGES)
                .build();
    }
}
