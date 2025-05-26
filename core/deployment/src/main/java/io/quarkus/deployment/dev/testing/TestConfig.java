package io.quarkus.deployment.dev.testing;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Testing
 * <p>
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.test.profile=someProfile or -Dquarkus.test.native-image-profile=someProfile
 * <p>
 * TODO refactor code to actually use these values
 */
@ConfigMapping(prefix = "quarkus.test")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TestConfig {

    /**
     * If continuous testing is enabled.
     * <p>
     * The default value is 'paused', which will allow you to start testing
     * from the console or the Dev UI, but will not run tests on startup.
     * <p>
     * If this is set to 'enabled' then testing will start as soon as the
     * application has started.
     * <p>
     * If this is 'disabled' then continuous testing is not enabled, and can't
     * be enabled without restarting the application.
     */
    @WithDefault("paused")
    Mode continuousTesting();

    /**
     * If output from the running tests should be displayed in the console.
     */
    @WithDefault("false")
    boolean displayTestOutput();

    /**
     * The FQCN of the JUnit <code>ClassOrderer</code> to use. If the class cannot be found, it fallbacks to JUnit
     * default behaviour which does not set a <code>ClassOrderer</code> at all.
     *
     * @see <a href=https://junit.org/junit5/docs/current/user-guide/#writing-tests-test-execution-order-classes>JUnit Class
     *      Order<a/>
     */
    @WithDefault("io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer")
    Optional<String> classOrderer();

    /**
     * Tags that should be included for continuous testing. This supports JUnit Tag Expressions.
     *
     * @see <a href="https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions">JUnit Tag Expressions</a>
     */
    Optional<List<String>> includeTags();

    /**
     * Tags that should be excluded by default with continuous testing.
     * <p>
     * This is ignored if include-tags has been set.
     * <p>
     * Defaults to 'slow'.
     * <p>
     * This supports JUnit Tag Expressions.
     *
     * @see <a href="https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions">JUnit Tag Expressions</a>
     */
    @WithDefault("slow")
    Optional<List<String>> excludeTags();

    /**
     * Tests that should be included for continuous testing. This is a regular expression and
     * is matched against the test class name (not the file name).
     */
    Optional<String> includePattern();

    /**
     * Tests that should be excluded with continuous testing. This is a regular expression and
     * is matched against the test class name (not the file name).
     * <p>
     * This is ignored if include-pattern has been set.
     */
    @WithDefault(".*\\.IT[^.]+|.*IT|.*ITCase")
    Optional<String> excludePattern();

    /**
     * Test engine ids that should be included for continuous testing.
     */
    Optional<List<String>> includeEngines();

    /**
     * Test engine ids that should be excluded by default with continuous testing.
     * <p>
     * This is ignored if include-engines has been set.
     */
    Optional<List<String>> excludeEngines();

    /**
     * Disable the testing status/prompt message at the bottom of the console
     * and log these messages to STDOUT instead.
     * <p>
     * Use this option if your terminal does not support ANSI escape sequences.
     * <p>
     * This is deprecated, {@literal quarkus.console.basic} should be used instead.
     */
    @Deprecated
    Optional<Boolean> basicConsole();

    /**
     * Disable color in the testing status and prompt messages.
     * <p>
     * Use this option if your terminal does not support color.
     * <p>
     * This is deprecated, {@literal quarkus.console.disable-color} should be used instead.
     */
    @Deprecated
    Optional<Boolean> disableColor();

    /**
     * If test results and status should be displayed in the console.
     * <p>
     * If this is false results can still be viewed in the dev console.
     * <p>
     * This is deprecated, {@literal quarkus.console.enabled} should be used instead.
     */
    @Deprecated
    Optional<Boolean> console();

    /**
     * Disables the ability to enter input on the console.
     * <p>
     * This is deprecated, {@literal quarkus.console.disable-input} should be used instead.
     */
    @Deprecated
    Optional<Boolean> disableConsoleInput();

    /**
     * Changes tests to use the 'flat' ClassPath used in Quarkus 1.x versions.
     * <p>
     * This means all Quarkus and test classes are loaded in the same ClassLoader,
     * however it means you cannot use continuous testing.
     * <p>
     * Note that if you find this necessary for your application then you
     * may also have problems running in development mode, which cannot use
     * a flat class path.
     */
    @WithDefault("false")
    boolean flatClassPath();

    /**
     * The profile to use when testing using {@code @QuarkusIntegrationTest}
     */
    @WithDefault("prod")
    String integrationTestProfile();

    /**
     * Profile related test settings
     */
    Profile profile();

    /**
     * Container related test settings
     */
    Container container();

    /**
     * Additional launch parameters to be used when Quarkus launches the produced artifact for {@code @QuarkusIntegrationTest}
     * When the artifact is a {@code jar}, this string is passed right after the {@code java} command.
     * When the artifact is a {@code container}, this string is passed right after the {@code docker run} command.
     * When the artifact is a {@code native binary}, this string is passed right after the native binary name.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> argLine();

    /**
     * Additional environment variables to be set in the process that {@code @QuarkusIntegrationTest} launches.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> env();

    /**
     * Used in {@code @QuarkusIntegrationTest} to determine how long the test will wait for the
     * application to launch
     */
    @WithDefault("PT1M")
    Duration waitTime();

    /**
     * Configures the hang detection in @QuarkusTest. If no activity happens (i.e. no test callbacks are called) over
     * this period then QuarkusTest will dump all threads stack traces, to help diagnose a potential hang.
     * <p>
     * Note that the initial timeout (before Quarkus has started) will only apply if provided by a system property, as
     * it is not possible to read all config sources until Quarkus has booted.
     */
    @WithDefault("10m")
    Duration hangDetectionTimeout();

    /**
     * The type of test to run, this can be either:
     * <p>
     * quarkus-test: Only runs {@code @QuarkusTest} annotated test classes
     * unit: Only runs classes that are not annotated with {@code @QuarkusTest}
     * all: Runs both, running the unit tests first
     *
     */
    @WithDefault("all")
    TestType type();

    /**
     * If this is true then only the tests from the main application module will be run (i.e. the module that is currently
     * running mvn quarkus:dev).
     * <p>
     * If this is false then tests from all dependency modules will be run as well.
     */
    @WithDefault("false")
    boolean onlyTestApplicationModule();

    /**
     * Modules that should be included for continuous testing. This is a regular expression and
     * is matched against the module groupId:artifactId.
     */
    Optional<String> includeModulePattern();

    /**
     * Modules that should be excluded for continuous testing. This is a regular expression and
     * is matched against the module groupId:artifactId.
     * <p>
     * This is ignored if include-module-pattern has been set.
     */
    Optional<String> excludeModulePattern();

    /**
     * If the test callbacks should be invoked for the integration tests (tests annotated with {@code @QuarkusIntegrationTest}).
     */
    @WithDefault("false")
    boolean enableCallbacksForIntegrationTests();

    /**
     * Used to override the artifact type against which a {@code @QuarkusIntegrationTest} or {@code @QuarkusMainIntegrationTest}
     * run.
     * For example, if the application's artifact is a container build from a jar, this property could be used to test the jar
     * instead of the container.
     * <p>
     * Allowed values are: jar, native
     */
    Optional<String> integrationTestArtifactType();

    interface Profile {
        /**
         * A comma separated list of profiles (dev, test, prod or custom profiles) to use when testing using @QuarkusTest
         */
        @WithParentName
        @WithDefault("test")
        List<String> profile();

        /**
         * The tags this profile is associated with.
         * When the {@code quarkus.test.profile.tags} System property is set (its value is a comma separated list of strings)
         * then Quarkus will only execute tests that are annotated with a {@code @TestProfile} that has at least one of the
         * supplied (via the aforementioned system property) tags.
         */
        Optional<List<@WithConverter(TrimmedStringConverter.class) String>> tags();
    }

    interface Container {
        /**
         * Controls the container network to be used when @QuarkusIntegration needs to launch the application in a container.
         * This setting only applies if Quarkus does not need to use a shared network - which is the case if DevServices are
         * used when running the test.
         */
        Optional<String> network();

        /**
         * Set additional ports to be exposed when @QuarkusIntegration needs to launch the application in a container.
         */
        @ConfigDocMapKey("host-port")
        Map<String, String> additionalExposedPorts();

        /**
         * A set of labels to add to the launched container
         */
        @ConfigDocMapKey("label-name")
        Map<String, String> labels();

        /**
         * A set of volume mounts to add to the launched container
         */
        @ConfigDocMapKey("host-path")
        Map<String, String> volumeMounts();
    }

    enum Mode {
        PAUSED,
        ENABLED,
        DISABLED
    }
}
