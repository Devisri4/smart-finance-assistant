package com.devisri.finance.assistant;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.devisri.finance.auth.User;
import com.devisri.finance.auth.UserRepository;

/**
 * Limits how often the chat endpoint can be used.
 * - Normal users: counted per account.
 * - The demo account is shared by every visitor, so it is counted per visitor IP instead.
 * - Everyone together: a daily cap that keeps us inside the AI provider's free quota.
 */
@Component
public class ChatRateLimits {

    static final String TOO_FAST = "You're sending messages too quickly. Please wait a minute and try again.";
    static final String DAILY_LIMIT = "You've reached the assistant's daily limit. Please try again later.";
    static final String EVERYONE_LIMIT =
            "The assistant has reached its daily limit for everyone (it runs on a free plan). Please try again later.";

    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration DAY = Duration.ofDays(1);

    private final UserRepository users;
    private final RateLimiter userPerMinute;
    private final RateLimiter userPerDay;
    private final RateLimiter demoIpPerMinute;
    private final RateLimiter demoIpPerDay;
    private final RateLimiter everyonePerDay;

    public ChatRateLimits(UserRepository users, Clock clock,
                          @Value("${app.assistant.limit.user-per-minute:10}") int userMinute,
                          @Value("${app.assistant.limit.user-per-day:60}") int userDay,
                          @Value("${app.assistant.limit.demo-ip-per-minute:5}") int demoMinute,
                          @Value("${app.assistant.limit.demo-ip-per-day:20}") int demoDay,
                          @Value("${app.assistant.limit.everyone-per-day:50}") int everyoneDay) {
        this.users = users;
        this.userPerMinute = new RateLimiter(userMinute, MINUTE, clock);
        this.userPerDay = new RateLimiter(userDay, DAY, clock);
        this.demoIpPerMinute = new RateLimiter(demoMinute, MINUTE, clock);
        this.demoIpPerDay = new RateLimiter(demoDay, DAY, clock);
        this.everyonePerDay = new RateLimiter(everyoneDay, DAY, clock);
    }

    /** Throws 429 Too Many Requests if this message is over a limit. */
    public void check(String userId, String clientIp) {
        boolean demo = users.findById(userId).map(User::isDemo).orElse(false);
        String key = demo ? "demo-ip:" + clientIp : "user:" + userId;
        if (!(demo ? demoIpPerMinute : userPerMinute).tryAcquire(key)) {
            throw tooMany(TOO_FAST);
        }
        if (!(demo ? demoIpPerDay : userPerDay).tryAcquire(key)) {
            throw tooMany(DAILY_LIMIT);
        }
        if (!everyonePerDay.tryAcquire("everyone")) {
            throw tooMany(EVERYONE_LIMIT);
        }
    }

    private static ResponseStatusException tooMany(String message) {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
