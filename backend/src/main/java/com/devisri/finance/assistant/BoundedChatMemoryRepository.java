package com.devisri.finance.assistant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Keeps recent chat conversations in memory. Spring AI's built-in in-memory store never forgets
 * anything, which would slowly fill a small free server, so this one keeps only the most recently
 * used conversations and drops the oldest. Everything is lost on restart (by design: short memory).
 */
@Component
public class BoundedChatMemoryRepository implements ChatMemoryRepository {

    private final Map<String, List<Message>> conversations;

    public BoundedChatMemoryRepository(@Value("${app.assistant.max-conversations:500}") int maxConversations) {
        // accessOrder = true: reading a conversation makes it "recently used".
        this.conversations = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<Message>> eldest) {
                return size() > maxConversations;
            }
        };
    }

    @Override
    public synchronized List<String> findConversationIds() {
        return new ArrayList<>(conversations.keySet());
    }

    @Override
    public synchronized List<Message> findByConversationId(String conversationId) {
        return new ArrayList<>(conversations.getOrDefault(conversationId, List.of()));
    }

    @Override
    public synchronized void saveAll(String conversationId, List<Message> messages) {
        conversations.put(conversationId, new ArrayList<>(messages));
    }

    @Override
    public synchronized void deleteByConversationId(String conversationId) {
        conversations.remove(conversationId);
    }
}
