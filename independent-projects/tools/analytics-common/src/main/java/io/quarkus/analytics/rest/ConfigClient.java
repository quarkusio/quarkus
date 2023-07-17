package io.quarkus.analytics.rest;

import java.util.Optional;

import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;

/**
 * Client to retrieve the analytics config from the upstream public location.
 */
public interface ConfigClient {
    Optional<AnalyticsRemoteConfig> getConfig();
}
