package com.devisri.finance.assistant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devisri.finance.assistant.AssistantService.ChatReply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    public static final int MAX_MESSAGE_LENGTH = 500;

    public record ChatRequest(
            @NotBlank(message = "Please type a message")
            @Size(max = MAX_MESSAGE_LENGTH, message = "Message is too long (max 500 characters)") String message) {
    }

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    /** The user id comes from the login token, never from the request body. */
    @PostMapping("/chat")
    ChatReply chat(@AuthenticationPrincipal String userId, @Valid @RequestBody ChatRequest request) {
        return assistant.chat(userId, request.message().trim());
    }
}
