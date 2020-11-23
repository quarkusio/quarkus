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
            case DefaultSentryClientFactory.ENVIRONMENT_OPTION:
                return config.environment.orElse(null);
            case DefaultSentryClientFactory.RELEASE_OPTION:
                return config.release.orElse(null);
            case DefaultSentryClientFactory.SERVERNAME_OPTION:
                return config.serverName.orElse(null);
            // New SentryConfig options should be mapped here
            default:
                return null;
        }
    }
}
