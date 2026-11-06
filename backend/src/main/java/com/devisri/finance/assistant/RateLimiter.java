package com.devisri.finance.assistant;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows at most "limit" requests per key in each time window, e.g. 10 per minute per user.
 * Counts are kept in memory, so they reset when the server restarts (fine for one small server).
 */
public class RateLimiter {

    /** Above this many keys, old windows are cleaned up so memory can't grow forever. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(long startMillis, int count) {
    }

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    /** Counts one request for this key. Returns false if the key is over its limit. */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.startMillis() >= windowMillis);
        }
        // compute() runs atomically per key, so two requests at the same time are both counted.
        Window window = windows.compute(key, (k, old) -> old == null || now - old.startMillis() >= windowMillis
                ? new Window(now, 1)
                : new Window(old.startMillis(), old.count() + 1));
        return window.count() <= limit;
    }
}
