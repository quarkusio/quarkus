package io.quarkus.test.junit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Defines a 'test profile'. Tests run under a test profile
 * will have different configuration options to other tests.
 *
 */
public interface QuarkusTestProfile {

    /**
     * Returns additional config to be applied to the test. This
     * will override any existing config (including in application.properties),
     * however existing config will be merged with this (i.e. application.properties
     * config will still take effect, unless a specific config key has been overridden).
     */
    default Map<String, String> getConfigOverrides() {
        return Collections.emptyMap();
    }

    /**
     * Returns enabled alternatives.
     *
     * This has the same effect as setting the 'quarkus.arc.selected-alternatives' config key,
     * however it may be more convenient.
     */
    default Set<Class<?>> getEnabledAlternatives() {
        return Collections.emptySet();
    }

    /**
     * Allows the default config profile to be overridden. This basically just sets the quarkus.test.profile system
     * property before the test is run.
     *
     */
    default String getConfigProfile() {
        return null;
    }

    /**
     * Additional {@link QuarkusTestResourceLifecycleManager} classes (along with their init params) to be used from this
     * specific test profile.
     *
     * If this method is not overridden, then only the {@link QuarkusTestResourceLifecycleManager} classes enabled via the
     * {@link io.quarkus.test.common.QuarkusTestResource} class
     * annotation will be used for the tests using this profile (which is the same behavior as tests that don't use a profile at
     * all).
     */
    default List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }

    /**
     * If this is returns true then only the test resources returned from {@link #testResources()} will be started,
     * global annotated test resources will be ignored.
     */
    default boolean disableGlobalTestResources() {
        return false;
    }

    /**
     * The tags this profile is associated with.
     * When the {@code quarkus.test.profile.tags} System property is set (its value is a comma separated list of strings)
     * then Quarkus will only execute tests that are annotated with a {@code @TestProfile} that has at least one of the
     * supplied (via the aforementioned system property) tags.
     */
    default Set<String> tags() {
        return Collections.emptySet();
    }

    /**
     * The command line parameters that are passed to the main method on startup.
     *
     * This is ignored for {@link io.quarkus.test.junit.main.QuarkusMainTest}, which has its own way of passing parameters.
     */
    default String[] commandLineParameters() {
        return new String[0];
    }

    /**
     * If the main method should be run.
     *
     * This is ignored for {@link io.quarkus.test.junit.main.QuarkusMainTest}, where the main method is always run.
     */
    default boolean runMainMethod() {
        return false;
    }

    /**
     * If this method returns true then all {@code StartupEvent} and {@code ShutdownEvent} observers declared on application
     * beans should be disabled.
     */
    default boolean disableApplicationLifecycleObservers() {
        return false;
    }

    final class TestResourceEntry {
        private final Class<? extends QuarkusTestResourceLifecycleManager> clazz;
        private final Map<String, String> args;
        private final boolean parallel;

        public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz) {
            this(clazz, Collections.emptyMap());
        }

        public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args) {
            this(clazz, args, false);
        }

        public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args,
                boolean parallel) {
            this.clazz = clazz;
            this.args = args;
            this.parallel = parallel;
        }

        public Class<? extends QuarkusTestResourceLifecycleManager> getClazz() {
            return clazz;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public boolean isParallel() {
            return parallel;
        }
    }
}
