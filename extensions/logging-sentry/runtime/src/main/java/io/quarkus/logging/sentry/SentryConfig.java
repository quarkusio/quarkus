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

    /**
     * Environment
     *
     * With Sentry you can easily filter issues, releases, and user feedback by environment.
     * The environment filter on sentry affects all issue-related metrics like count of users affected, times series graphs,
     * and event count.
     * By setting the environment option, an environment tag will be added to each new issue sent to Sentry.
     *
     * There are a few restrictions:
     * -> the environment name cannot contain newlines or spaces, cannot be the string “None” or exceed 64 characters.
     *
     */
    @ConfigItem
    public Optional<String> environment;

    /**
     * Release
     *
     * A release is a version of your code that is deployed to an environment.
     * When you give Sentry information about your releases, you unlock a number of new features:
     * - Determine the issues and regressions introduced in a new release
     * - Predict which commit caused an issue and who is likely responsible
     * - Resolve issues by including the issue number in your commit message
     * - Receive email notifications when your code gets deployed
     *
     */
    @ConfigItem
    public Optional<String> release;

    /**
     * Server name
     *
     * Sets the server name that will be sent with each event.
     */
    @ConfigItem
    public Optional<String> serverName;
}
