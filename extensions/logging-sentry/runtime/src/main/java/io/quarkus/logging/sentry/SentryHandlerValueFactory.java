package io.quarkus.logging.sentry;

import static java.lang.String.join;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.sentry.Sentry;
import io.sentry.SentryOptions;

@Recorder
public class SentryHandlerValueFactory {
    private static final Logger LOG = Logger.getLogger(SentryHandlerValueFactory.class);

    public RuntimeValue<Optional<Handler>> create(final SentryConfig config) {

        if (!config.enable) {
            return new RuntimeValue<>(Optional.empty());
        }

        // Init Sentry
        final SentryOptions options = toSentryOptions(config);
        Sentry.init(options);
        SentryHandler handler = new SentryHandler(options);
        handler.setLevel(config.level);
        handler.setPrintfStyle(true);
        return new RuntimeValue<>(Optional.of(handler));
    }

    public static SentryOptions toSentryOptions(SentryConfig sentryConfig) {
        if (!sentryConfig.dsn.isPresent()) {
            throw new ConfigurationException(
                    "Configuration key \"quarkus.log.sentry.dsn\" is required when Sentry is enabled, but its value is empty/missing");
        }
        if (!sentryConfig.inAppPackages.isPresent()) {
            LOG.warn(
                    "No 'quarkus.sentry.in-app-packages' was configured, this option is highly recommended as it affects stacktrace grouping and display on Sentry. See https://quarkus.io/guides/logging-sentry#in-app-packages");
        }

        final SentryOptions options = new SentryOptions();
        options.setDsn(sentryConfig.dsn.get());
        sentryConfig.inAppPackages
                .map(p -> join(",", p))
                .filter(s -> !Objects.equals(s, "*"))
                .ifPresent(options::addInAppInclude);
        sentryConfig.environment.ifPresent(options::setEnvironment);
        sentryConfig.release.ifPresent(options::setRelease);
        sentryConfig.serverName.ifPresent(options::setServerName);
        return options;
    }
}
