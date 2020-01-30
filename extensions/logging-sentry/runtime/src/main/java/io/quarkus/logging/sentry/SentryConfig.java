package io.quarkus.logging.sentry;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for Sentry logging.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.sentry")
public class SentryConfig {

    /**
     * Determine whether to enable the Sentry logging extension.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    boolean enable;

    /**
     * Sentry DSN
     *
     * The DSN is the first and most important thing to configure because it tells the SDK where to send events. You can find
     * your project’s DSN in the “Client Keys” section of your “Project Settings” in Sentry.
     */
    @ConfigItem
    public Optional<String> dsn;

    /**
     * The sentry log level.
     */
    @ConfigItem(defaultValue = "WARN")
    public Level level;

    /**
     * Sentry differentiates stack frames that are directly related to your application (“in application”) from stack frames
     * that come from other packages such as the standard library, frameworks, or other dependencies. The difference is visible
     * in the Sentry web interface where only the “in application” frames are displayed by default.
     *
     * You can configure which package prefixes your application uses with this option.
     *
     * This option is highly recommended as it affects stacktrace grouping and display on Sentry. See documentation:
     * https://quarkus.io/guides/logging-sentry#in-app-packages
     */
    @ConfigItem
    public Optional<List<String>> inAppPackages;
}
