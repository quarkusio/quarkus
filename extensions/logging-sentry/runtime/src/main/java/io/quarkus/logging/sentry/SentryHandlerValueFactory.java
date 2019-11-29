package io.quarkus.logging.sentry;

import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.config.Lookup;
import io.sentry.jul.SentryHandler;

@Recorder
public class SentryHandlerValueFactory {
    private static final Logger LOG = Logger.getLogger(SentryConfigProvider.class);

    public RuntimeValue<Optional<Handler>> create(final SentryConfig config) {
        if (!config.enable) {
            return new RuntimeValue<>(Optional.empty());
        }
        if (!config.inAppPackages.isPresent()) {
            LOG.warn(
                    "No 'quarkus.sentry.in-app-packages' was configured, this option is highly recommended as it affects stacktrace grouping and display on Sentry.");
        }
        final SentryConfigProvider provider = new SentryConfigProvider(config);
        Sentry.init(SentryOptions.from(new Lookup(provider, provider), config.dsn));
        SentryHandler handler = new SentryHandler();
        handler.setLevel(config.level);
        return new RuntimeValue<>(Optional.of(handler));
    }
}
