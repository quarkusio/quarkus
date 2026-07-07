package io.quarkus.gradle.application.internal.plugin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmStartupArchive;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.dsl.QuarkusApplicationTests;

/**
 * Coordinates configuration-time lifecycle transitions for one plugin-applied project.
 * This type is public only so Gradle-managed DSL implementations in another package can
 * delegate to it. Its instance is an unreachable plugin implementation detail.
 */
public final class DslLifecycleCoordinator {

    private final Map<QuarkusAotJarOutput, EventState<QuarkusAotJarOutput>> optimizedImages = new IdentityHashMap<>();
    private final Map<QuarkusApplicationJvmStartupArchive, StartupArchiveState> startupArchives = new IdentityHashMap<>();
    private final Map<QuarkusApplicationTests, ApplicationTestsState> applicationTests = new IdentityHashMap<>();
    private final Map<QuarkusApplicationJvmTestSuite, JvmSuiteState> jvmSuites = new IdentityHashMap<>();

    DslLifecycleCoordinator() {
    }

    public void whenStartupOptimizedImageConfigured(QuarkusAotJarOutput build,
            Action<? super QuarkusAotJarOutput> action) {
        subscribe(eventState(optimizedImages, build), build, action);
    }

    public void startupOptimizedImageConfigured(QuarkusAotJarOutput build) {
        deliver(eventState(optimizedImages, build), build);
    }

    public void whenPackageBuildConfigured(QuarkusApplicationJvmStartupArchive archive,
            Action<? super QuarkusApplicationJvmStartupArchive> action) {
        subscribe(startupArchiveState(archive).packageBuild, archive, action);
    }

    public void packageBuildConfigured(QuarkusApplicationJvmStartupArchive archive, String buildName) {
        EventState<QuarkusApplicationJvmStartupArchive> event;
        synchronized (this) {
            StartupArchiveState state = startupArchiveState(archive);
            if (state.owner == StartupArchiveOwner.INTEGRATION_TRAINING) {
                throw startupArchiveOwnerConflict(buildName);
            }
            state.owner = StartupArchiveOwner.PACKAGE_BUILD;
            event = state.packageBuild;
        }
        deliver(event, archive);
    }

    public void claimIntegrationTestTraining(QuarkusApplicationJvmStartupArchive archive, String buildName) {
        synchronized (this) {
            StartupArchiveState state = startupArchiveState(archive);
            if (state.owner != StartupArchiveOwner.UNCLAIMED) {
                throw startupArchiveOwnerConflict(buildName);
            }
            state.owner = StartupArchiveOwner.INTEGRATION_TRAINING;
        }
    }

    public void selectTestTask(QuarkusApplicationTests tests, TaskProvider<? extends Test> taskProvider) {
        Action<TaskProvider<? extends Test>> configurer = null;
        synchronized (this) {
            ApplicationTestsState state = applicationTestsState(tests);
            if (state.selectedIdentity.put(taskProvider, Boolean.TRUE) != null) {
                return;
            }
            if (state.taskConfigurer == null) {
                state.selected.add(taskProvider);
            } else {
                configurer = state.taskConfigurer;
            }
        }
        if (configurer != null) {
            configurer.execute(taskProvider);
        }
    }

    public void selectAllGradleTestTasks(QuarkusApplicationTests tests) {
        Runnable configurer = null;
        synchronized (this) {
            ApplicationTestsState state = applicationTestsState(tests);
            if (state.allGradleTestTasks) {
                return;
            }
            state.allGradleTestTasks = true;
            configurer = state.allGradleTestTasksConfigurer;
        }
        if (configurer != null) {
            configurer.run();
        }
    }

    public void attachTests(QuarkusApplicationTests tests,
            Action<TaskProvider<? extends Test>> taskConfigurer,
            Runnable allGradleTestTasksConfigurer) {
        List<TaskProvider<? extends Test>> selected;
        boolean configureAll;
        synchronized (this) {
            ApplicationTestsState state = applicationTestsState(tests);
            if (state.taskConfigurer != null) {
                throw new IllegalStateException("Quarkus application tests already have an internal configurator");
            }
            state.taskConfigurer = taskConfigurer;
            state.allGradleTestTasksConfigurer = allGradleTestTasksConfigurer;
            selected = List.copyOf(state.selected);
            state.selected.clear();
            configureAll = state.allGradleTestTasks;
        }
        selected.forEach(taskConfigurer::execute);
        if (configureAll) {
            allGradleTestTasksConfigurer.run();
        }
    }

    public boolean claimQuarkusTests(QuarkusApplicationJvmTestSuite suite, String suiteName) {
        synchronized (this) {
            JvmSuiteState state = jvmSuiteState(suite);
            rejectNativeSuite(state, suiteName, "forQuarkusTests()");
            if (state.mode == JvmSuiteMode.INTEGRATION) {
                throw new GradleException(
                        "A JVM test suite cannot be configured for both Quarkus JVM and integration tests.");
            }
            if (state.startupArchiveTraining) {
                throw new GradleException(
                        "JVM startup-archive training requires a Quarkus integration-test suite.");
            }
            if (state.mode == JvmSuiteMode.JVM) {
                return false;
            }
            state.mode = JvmSuiteMode.JVM;
            return true;
        }
    }

    public void claimQuarkusIntegrationTests(QuarkusApplicationJvmTestSuite suite, String suiteName) {
        synchronized (this) {
            JvmSuiteState state = jvmSuiteState(suite);
            rejectNativeSuite(state, suiteName, "forQuarkusIntegrationTests(...)");
            if (state.mode == JvmSuiteMode.JVM) {
                throw new GradleException(
                        "A JVM test suite cannot be configured for both Quarkus JVM and integration tests.");
            }
            if (state.mode != JvmSuiteMode.UNCONFIGURED) {
                throw new GradleException("A JVM test suite cannot be configured for more than one Quarkus test mode.");
            }
            state.mode = JvmSuiteMode.INTEGRATION;
        }
    }

    public void claimStartupArchiveTraining(QuarkusApplicationJvmTestSuite suite, String suiteName) {
        synchronized (this) {
            JvmSuiteState state = jvmSuiteState(suite);
            rejectNativeSuite(state, suiteName, "startupArchiveTraining(...)");
            if (state.mode == JvmSuiteMode.JVM) {
                throw new GradleException(
                        "JVM startup-archive training requires a Quarkus integration-test suite.");
            }
            if (state.startupArchiveTraining) {
                throw new GradleException(
                        "A JVM test suite cannot configure startup-archive training more than once.");
            }
            state.startupArchiveTraining = true;
        }
    }

    public void claimNamedNativeTestSuite(QuarkusApplicationJvmTestSuite suite, String suiteName) {
        synchronized (this) {
            JvmSuiteState state = jvmSuiteState(suite);
            if (state.mode != JvmSuiteMode.UNCONFIGURED) {
                throw new GradleException("JVM test suite '" + suiteName
                        + "' cannot be claimed as a generated Quarkus named native-test suite after its test mode was configured");
            }
            state.mode = JvmSuiteMode.NAMED_NATIVE;
        }
    }

    private <T> void subscribe(EventState<T> event, T subject, Action<? super T> action) {
        boolean execute;
        synchronized (this) {
            execute = event.delivered;
            if (!execute) {
                event.actions.add(action);
            }
        }
        if (execute) {
            action.execute(subject);
        }
    }

    private <T> void deliver(EventState<T> event, T subject) {
        List<Action<? super T>> actions;
        synchronized (this) {
            if (event.delivered) {
                return;
            }
            event.delivered = true;
            actions = List.copyOf(event.actions);
            event.actions.clear();
        }
        actions.forEach(action -> action.execute(subject));
    }

    private synchronized StartupArchiveState startupArchiveState(QuarkusApplicationJvmStartupArchive archive) {
        return startupArchives.computeIfAbsent(archive, ignored -> new StartupArchiveState());
    }

    private synchronized ApplicationTestsState applicationTestsState(QuarkusApplicationTests tests) {
        return applicationTests.computeIfAbsent(tests, ignored -> new ApplicationTestsState());
    }

    private synchronized JvmSuiteState jvmSuiteState(QuarkusApplicationJvmTestSuite suite) {
        return jvmSuites.computeIfAbsent(suite, ignored -> new JvmSuiteState());
    }

    private synchronized <T> EventState<T> eventState(Map<T, EventState<T>> states, T subject) {
        return states.computeIfAbsent(subject, ignored -> new EventState<>());
    }

    private static void rejectNativeSuite(JvmSuiteState state, String suiteName, String operation) {
        if (state.mode == JvmSuiteMode.NAMED_NATIVE) {
            throw new GradleException("JVM test suite '" + suiteName
                    + "' is a generated Quarkus named native-test suite already attached to its matching native build; "
                    + operation + " cannot change its test mode");
        }
    }

    private static GradleException startupArchiveOwnerConflict(String buildName) {
        return new GradleException("Quarkus AOT-JAR output '" + buildName
                + "' cannot use package and integration-test startup archive producers together");
    }

    private static final class EventState<T> {
        private final List<Action<? super T>> actions = new ArrayList<>();
        private boolean delivered;
    }

    private static final class StartupArchiveState {
        private final EventState<QuarkusApplicationJvmStartupArchive> packageBuild = new EventState<>();
        private StartupArchiveOwner owner = StartupArchiveOwner.UNCLAIMED;
    }

    private enum StartupArchiveOwner {
        UNCLAIMED,
        PACKAGE_BUILD,
        INTEGRATION_TRAINING
    }

    private static final class ApplicationTestsState {
        private final List<TaskProvider<? extends Test>> selected = new ArrayList<>();
        private final IdentityHashMap<TaskProvider<? extends Test>, Boolean> selectedIdentity = new IdentityHashMap<>();
        private Action<TaskProvider<? extends Test>> taskConfigurer;
        private Runnable allGradleTestTasksConfigurer;
        private boolean allGradleTestTasks;
    }

    private static final class JvmSuiteState {
        private JvmSuiteMode mode = JvmSuiteMode.UNCONFIGURED;
        private boolean startupArchiveTraining;
    }

    private enum JvmSuiteMode {
        UNCONFIGURED,
        JVM,
        INTEGRATION,
        NAMED_NATIVE
    }
}
