package io.quarkus.logging.sentry;

import static java.lang.String.join;

import java.util.Objects;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.config.provider.ConfigurationProvider;

/**
 * Mapping between the SentryConfig and the Sentry options {@link io.sentry.DefaultSentryClientFactory}
 */
class SentryConfigProvider implements ConfigurationProvider {

    private final SentryConfig config;

    SentryConfigProvider(SentryConfig config) {
        this.config = config;
    }

    @Override
    public String getProperty(String key) {
        switch (key) {
            case DefaultSentryClientFactory.IN_APP_FRAMES_OPTION:
                return config.inAppPackages.map(p -> join(",", p))
                        .filter(s -> !Objects.equals(s, "*"))
                        .orElse("");
            // New SentryConfig options should be mapped here
            default:
                return null;
        }
    }
}
