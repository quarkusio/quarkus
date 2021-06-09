package io.quarkus.deployment.dev.testing;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.test.profile=someProfile or -Dquarkus.test.native-image-profile=someProfile
 * <p>
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class TestConfig {

    /**
     * If continuous testing is enabled.
     *
     * The default value is 'paused', which will allow you to start testing
     * from the console or the Dev UI, but will not run tests on startup.
     *
     * If this is set to 'enabled' then testing will start as soon as the
     * application has started.
     *
     * If this is 'disabled' then continuous testing is not enabled, and can't
     * be enabled without restarting the application.
     *
     */
    @ConfigItem(defaultValue = "paused")
    public Mode continuousTesting;

    /**
     * If output from the running tests should be displayed in the console.
     */
    @ConfigItem(defaultValue = "false")
    public boolean displayTestOutput;

    /**
     * Tags that should be included for continuous testing.
     */
    @ConfigItem
    public Optional<List<String>> includeTags;

    /**
     * Tags that should be excluded by default with continuous testing.
     *
     * This is ignored if include-tags has been set.
     *
     * Defaults to 'slow'
     */
    @ConfigItem(defaultValue = "slow")
    public Optional<List<String>> excludeTags;

    /**
     * Tests that should be included for continuous testing. This is a regular expression and
     * is matched against the test class name (not the file name).
     */
    @ConfigItem
    public Optional<String> includePattern;

    /**
     * Tests that should be excluded with continuous testing. This is a regular expression and
     * is matched against the test class name (not the file name).
     *
     * This is ignored if include-pattern has been set.
     *
     */
    @ConfigItem(defaultValue = ".*\\.IT[^.]+|.*IT|.*ITCase")
    public Optional<String> excludePattern;
    /**
     * Disable the testing status/prompt message at the bottom of the console
     * and log these messages to STDOUT instead.
     *
     * Use this option if your terminal does not support ANSI escape sequences.
     */
    @ConfigItem(defaultValue = "false")
    public boolean basicConsole;

    /**
     * Disable color in the testing status and prompt messages.
     *
     * Use this option if your terminal does not support color.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableColor;

    /**
     * If test results and status should be displayed in the console.
     *
     * If this is false results can still be viewed in the dev console.
     */
    @ConfigItem(defaultValue = "true")
    public boolean console;

    /**
     * Disables the ability to enter input on the console.
     *
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableConsoleInput;

    /**
     * Changes tests to use the 'flat' ClassPath used in Quarkus 1.x versions.
     *
     * This means all Quarkus and test classes are loaded in the same ClassLoader,
     * however it means you cannot use continuous testing.
     *
     * Note that if you find this necessary for your application then you
     * may also have problems running in development mode, which cannot use
     * a flat class path.
     */
    @ConfigItem(defaultValue = "false")
    public boolean flatClassPath;
    /**
     * Duration to wait for the native image to built during testing
     */
    @ConfigItem(defaultValue = "PT5M")
    Duration nativeImageWaitTime;

    /**
     * The profile to use when testing the native image
     */
    @ConfigItem(defaultValue = "prod")
    String nativeImageProfile;

    /**
     * Profile related test settings
     */
    @ConfigItem
    Profile profile;

    /**
     * JVM parameters that are used to launch jar based integration tests.
     */
    @ConfigItem
    Optional<String> integrationJvmArgLine;

    /**
     * Configures the hang detection in @QuarkusTest. If no activity happens (i.e. no test callbacks are called) over
     * this period then QuarkusTest will dump all threads stack traces, to help diagnose a potential hang.
     *
     * Note that the initial timeout (before Quarkus has started) will only apply if provided by a system property, as
     * it is not possible to read all config sources until Quarkus has booted.
     */
    @ConfigItem(defaultValue = "10m")
    Duration hangDetectionTimeout;

    /**
     * The type of test to run, this can be either:
     *
     * quarkus-test: Only runs {@code @QuarkusTest} annotated test classes
     * unit: Only runs classes that are not annotated with {@code @QuarkusTest}
     * all: Runs both, running the unit tests first
     *
     */
    @ConfigItem(defaultValue = "all")
    TestType type;

    /**
     * If a class matches this pattern then it will be cloned into the Quarkus ClassLoader even if it
     * is in a parent first artifact.
     *
     * This is important for collections which can contain objects from the Quarkus ClassLoader, but for
     * most parent first classes it will just cause problems.
     */
    @ConfigItem(defaultValue = "java\\..*")
    String classClonePattern;

    @ConfigGroup
    public static class Profile {

        /**
         * The profile (dev, test or prod) to use when testing using @QuarkusTest
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "test")
        String profile;

        /**
         * The tags this profile is associated with.
         * When the {@code quarkus.test.profile.tags} System property is set (its value is a comma separated list of strings)
         * then Quarkus will only execute tests that are annotated with a {@code @TestProfile} that has at least one of the
         * supplied (via the aforementioned system property) tags.
         */
        @ConfigItem(defaultValue = "")
        Optional<List<String>> tags;
    }

    public enum Mode {
        PAUSED,
        ENABLED,
        DISABLED
    }
}
