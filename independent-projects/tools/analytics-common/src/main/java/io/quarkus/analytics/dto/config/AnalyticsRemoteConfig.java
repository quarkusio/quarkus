package io.quarkus.analytics.dto.config;

import java.time.Duration;
import java.util.List;

/**
 * Allow to configure build analytics behaviour
 */
public interface AnalyticsRemoteConfig {
    /**
     * @return true if the analytics is enabled
     * @return
     */
    boolean isActive();

    /**
     * List of anonymous UUID representing the users who will not send analytics.
     * The data from particular UUIDs might contain issues and generation will be disabled at the source.
     *
     * @return
     */
    List<String> getDenyAnonymousIds();

    /**
     * List of quarkus versions that will not send analytics.
     * The data from particular versions might contain issues and generation will be disabled at the source.
     *
     * @return
     */
    List<String> getDenyQuarkusVersions();

    /**
     * Configuration refresh interval
     *
     * @return
     */
    Duration getRefreshInterval();
}
