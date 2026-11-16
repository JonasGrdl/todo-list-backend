package com.example.demo;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class Limits {
    private record Window(Instant end, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private Instant nextCleanup = Instant.MIN;

    Limits(Clock clock) { this.clock = clock; }

    synchronized void check(String key, int maximum, int seconds) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            windows.values().removeIf(w -> !now.isBefore(w.end()));
            nextCleanup = now.plusSeconds(60);
        }
        Window w = windows.get(key);
        if (w == null || !now.isBefore(w.end())) w = new Window(now.plusSeconds(seconds), 0);
        if (w.count() >= maximum || (windows.size() >= 50000 && !windows.containsKey(key))) throw ApiError.limited();
        windows.put(key, new Window(w.end(), w.count() + 1));
    }
}
