package com.devisri.finance.assistant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devisri.finance.assistant.AssistantService.ChatReply;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    public static final int MAX_MESSAGE_LENGTH = 500;

    /** conversationId is optional: a random id the browser makes for each new chat. */
    public record ChatRequest(
            @NotBlank(message = "Please type a message")
            @Size(max = MAX_MESSAGE_LENGTH, message = "Message is too long (max 500 characters)") String message,
            @Pattern(regexp = "[A-Za-z0-9-]{1,64}", message = "Invalid conversation id") String conversationId) {
    }

    private final AssistantService assistant;
    private final ChatRateLimits rateLimits;
    private final ClientIp clientIp;

    public AssistantController(AssistantService assistant, ChatRateLimits rateLimits, ClientIp clientIp) {
        this.assistant = assistant;
        this.rateLimits = rateLimits;
        this.clientIp = clientIp;
    }

    /** The user id comes from the login token, never from the request body. */
    @PostMapping("/chat")
    ChatReply chat(@AuthenticationPrincipal String userId, @Valid @RequestBody ChatRequest request,
                   HttpServletRequest http) {
        rateLimits.check(userId, clientIp.of(http));
        return assistant.chat(userId, request.conversationId(), request.message().trim());
    }
}
