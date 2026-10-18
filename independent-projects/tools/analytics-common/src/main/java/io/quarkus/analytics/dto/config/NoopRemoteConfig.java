package io.quarkus.analytics.dto.config;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Will not perform any operations
 */
public class NoopRemoteConfig implements AnalyticsRemoteConfig {
    private static final Duration DONT_CHECK_ANYMORE = Duration.ofDays(365);
    public static final NoopRemoteConfig INSTANCE = new NoopRemoteConfig();

    private NoopRemoteConfig() {
        // singleton
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public List<String> getDenyAnonymousIds() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getDenyQuarkusVersions() {
        return Collections.emptyList();
    }

    @Override
    public Duration getRefreshInterval() {
        return DONT_CHECK_ANYMORE;
    }
}
