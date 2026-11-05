package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** A clock the test can move forward by hand. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-03-15T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    void allowsUpToTheLimitThenBlocks() {
        assertThat(limiter.tryAcquire("user:a")).isTrue();
        assertThat(limiter.tryAcquire("user:a")).isTrue();
        assertThat(limiter.tryAcquire("user:a")).isTrue();
        assertThat(limiter.tryAcquire("user:a")).isFalse();
    }

    @Test
    void eachKeyHasItsOwnCount() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("user:a");
        }
        assertThat(limiter.tryAcquire("user:a")).isFalse();
        assertThat(limiter.tryAcquire("user:b")).isTrue();
    }

    @Test
    void theCountStartsAgainInTheNextWindow() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("user:a");
        }
        clock.now = clock.now.plusSeconds(61);

        assertThat(limiter.tryAcquire("user:a")).isTrue();
    }
}
