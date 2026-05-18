package com.enterprise.timeout.alert.suppression;

import com.enterprise.timeout.config.GovernanceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AlertSuppressor {

    private final GovernanceProperties properties;
    private final ConcurrentHashMap<String, LocalDateTime> suppressionMap = new ConcurrentHashMap<>();

    public boolean isSuppressed(String key) {
        LocalDateTime lastSent = suppressionMap.get(key);
        if (lastSent == null) {
            return false;
        }

        int windowMinutes = properties.getAlert().getSuppression().getDefaultWindowMinutes();
        return lastSent.plusMinutes(windowMinutes).isAfter(LocalDateTime.now());
    }

    public void markSent(String key) {
        suppressionMap.put(key, LocalDateTime.now());
    }

    public void clear(String key) {
        suppressionMap.remove(key);
    }

    public void clearExpired() {
        int windowMinutes = properties.getAlert().getSuppression().getDefaultWindowMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(windowMinutes);
        suppressionMap.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
