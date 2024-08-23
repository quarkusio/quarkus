package io.quarkus.analytics.common;

import java.util.Optional;

import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;
import io.quarkus.analytics.rest.ConfigClient;

public class TestRestClient implements ConfigClient {

    private AnalyticsRemoteConfig analyticsRemoteConfig;

    public TestRestClient(AnalyticsRemoteConfig analyticsRemoteConfig) {
        this.analyticsRemoteConfig = analyticsRemoteConfig;
    }

    @Override
    public Optional<AnalyticsRemoteConfig> getConfig() {
        return Optional.of(analyticsRemoteConfig);
    }
}
