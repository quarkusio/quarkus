package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import groovy.lang.Closure;
import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;

/**
 * Adds Quarkus test modes to one Gradle {@link JvmTestSuite}.
 * <p>
 * The standalone plugin creates this extension on every JVM test suite under the name {@code quarkusApplication} and
 * also installs Groovy/Kotlin-friendly callable entries. A suite may be selected once as ordinary Quarkus JVM tests or
 * once as integration tests for one named application build; those modes are mutually exclusive. Startup-archive
 * training additionally requires integration-test mode. Generated named native-test suites already have their mode
 * fixed and support only {@link #includeTestsFrom(NamedDomainObjectProvider)}.
 */
public class QuarkusApplicationJvmTestSuite {

    private final transient JvmTestSuite suite;
    private final transient Action<TaskProvider<? extends Test>> configureTestTask;
    private final transient Action<IntegrationTestRequest> configureIntegrationTestSuite;
    private final transient Action<StartupArchiveTrainingRequest> configureStartupArchiveTraining;
    private final transient Action<IncludedTestsRequest> configureIncludedTests;
    private final QuarkusApplicationStartupArchiveTraining startupArchiveTraining;
    private final DslLifecycleCoordinator lifecycle;

    /**
     * Creates the Gradle-managed suite extension and callback bridge.
     * <p>
     * This constructor is plugin wiring; build scripts use the suite methods and callable extensions instead.
     *
     * @param objects Gradle's object factory
     * @param suite the owning JVM test suite
     * @param configureTestTask the plugin callback for ordinary Quarkus tests
     * @param configureIntegrationTestSuite the plugin callback for integration tests
     * @param configureStartupArchiveTraining the plugin callback for archive training
     * @param configureIncludedTests the plugin callback for included native-test sources
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     */
    @Inject
    public QuarkusApplicationJvmTestSuite(ObjectFactory objects, JvmTestSuite suite,
            Action<TaskProvider<? extends Test>> configureTestTask,
            Action<IntegrationTestRequest> configureIntegrationTestSuite,
            Action<StartupArchiveTrainingRequest> configureStartupArchiveTraining,
            Action<IncludedTestsRequest> configureIncludedTests,
            Object lifecycle) {
        this.suite = suite;
        this.configureTestTask = configureTestTask;
        this.configureIntegrationTestSuite = configureIntegrationTestSuite;
        this.configureStartupArchiveTraining = configureStartupArchiveTraining;
        this.configureIncludedTests = configureIncludedTests;
        if (!(lifecycle instanceof DslLifecycleCoordinator coordinator)) {
            throw new IllegalArgumentException("Quarkus JVM test suite requires its internal lifecycle coordinator");
        }
        this.lifecycle = coordinator;
        this.startupArchiveTraining = objects.newInstance(QuarkusApplicationStartupArchiveTraining.class);
    }

    /**
     * Configures this suite's targets as ordinary Quarkus JVM tests.
     * <p>
     * Repeated calls are idempotent. This mode cannot be combined with integration tests, startup-archive training, or
     * a generated named native-test suite. Kotlin build scripts can call {@code forQuarkusTests()}; Groovy build scripts
     * use the identically named callable extension.
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void forQuarkusTests() {
        if (lifecycle.claimQuarkusTests(this, suite.getName())) {
            suite.getTargets().configureEach(target -> configureTestTask.execute(target.getTestTask()));
        }
    }

    /**
     * Configures this suite as integration tests for one named application build.
     * <p>
     * Accepted notation is a non-empty build name, a {@link QuarkusApplicationBuild}, a
     * {@link org.gradle.api.provider.Provider} producing either, or a named-domain-object provider. Resolution and task
     * wiring remain lazy. This mode cannot be combined with ordinary Quarkus JVM tests or another integration-test
     * target.
     *
     * @param build the named-build notation
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void forQuarkusIntegrationTests(Object build) {
        lifecycle.claimQuarkusIntegrationTests(this, suite.getName());
        configureIntegrationTestSuite.execute(new IntegrationTestRequest(suite, build));
    }

    /**
     * Configures this integration-test suite to train the selected AOT-JAR build's startup archive.
     * <p>
     * Exactly one execution target must be set by the action. The training declaration may appear before or after
     * {@link #forQuarkusIntegrationTests(Object)}, but the suite must ultimately have exactly one integration-test
     * target and cannot use ordinary JVM-test or generated native-test mode.
     *
     * @param action the training configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void startupArchiveTraining(Action<? super QuarkusApplicationStartupArchiveTraining> action) {
        lifecycle.claimStartupArchiveTraining(this, suite.getName());
        action.execute(startupArchiveTraining);
        if (!startupArchiveTraining.getExecutionTarget().isPresent()) {
            throw new GradleException("JVM startup-archive training requires an explicit execution target.");
        }
        startupArchiveTraining.getExecutionTarget().disallowChanges();
        configureStartupArchiveTraining.execute(new StartupArchiveTrainingRequest(suite, startupArchiveTraining));
    }

    /**
     * Adds another project-owned JVM test suite's compiled tests and runtime dependencies to this generated named
     * native-test suite.
     * <p>
     * The referenced suite remains lazily selected and duplicate declarations are idempotent. This operation is
     * rejected for ordinary user-defined JVM and integration-test suites, self-inclusion, inclusion of another
     * generated native-test suite, and suites from another project.
     *
     * @param includedSuite the project-owned suite whose tests should be included
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void includeTestsFrom(NamedDomainObjectProvider<? extends JvmTestSuite> includedSuite) {
        configureIncludedTests.execute(new IncludedTestsRequest(suite, includedSuite));
    }

    /**
     * Internal callback payload connecting a JVM suite to its named-build notation.
     * <p>
     * This record is public only because Gradle-managed constructor injection crosses package boundaries. It is not a
     * supported user DSL entry point.
     *
     * @param suite the JVM test suite being configured
     * @param build the accepted named-build notation
     */
    public record IntegrationTestRequest(JvmTestSuite suite, Object build) {
    }

    /**
     * Internal callback payload connecting a JVM suite to startup-archive training.
     * <p>
     * This record is public only for Gradle-managed callback wiring and is not a supported user DSL entry point.
     *
     * @param suite the JVM test suite being configured
     * @param training the training configuration owned by the suite extension
     */
    public record StartupArchiveTrainingRequest(
            JvmTestSuite suite,
            QuarkusApplicationStartupArchiveTraining training) {
    }

    /**
     * Internal callback payload connecting a generated named native-test suite to an included JVM suite.
     * <p>
     * This record is public only for Gradle-managed callback wiring and is not a supported user DSL entry point.
     *
     * @param suite the generated named native-test suite
     * @param includedSuite the lazily referenced project-owned JVM suite
     */
    public record IncludedTestsRequest(
            JvmTestSuite suite,
            NamedDomainObjectProvider<? extends JvmTestSuite> includedSuite) {
    }

    /**
     * Callable adapter installed as {@code forQuarkusTests} on a JVM test suite.
     * <p>
     * {@link #invoke()} supports Kotlin invocation and {@link #call()} supports Groovy invocation.
     */
    public static final class QuarkusTests {

        private final QuarkusApplicationJvmTestSuite delegate;

        /**
         * Creates the callable adapter.
         *
         * @param delegate the owning Quarkus suite extension
         */
        public QuarkusTests(QuarkusApplicationJvmTestSuite delegate) {
            this.delegate = delegate;
        }

        /**
         * Selects ordinary Quarkus JVM-test mode from Kotlin DSL.
         */
        @SuppressWarnings("unused") // Kotlin DSL invoke convention
        public void invoke() {
            delegate.forQuarkusTests();
        }

        /**
         * Selects ordinary Quarkus JVM-test mode from Groovy DSL.
         */
        @SuppressWarnings("unused") // Groovy DSL call convention
        public void call() {
            delegate.forQuarkusTests();
        }
    }

    /**
     * Callable adapter installed as {@code forQuarkusIntegrationTests} on a JVM test suite.
     * <p>
     * {@link #invoke(Object)} supports Kotlin invocation and {@link #call(Object)} supports Groovy invocation.
     */
    public static final class QuarkusIntegrationTests {

        private final QuarkusApplicationJvmTestSuite delegate;

        /**
         * Creates the callable adapter.
         *
         * @param delegate the owning Quarkus suite extension
         */
        public QuarkusIntegrationTests(QuarkusApplicationJvmTestSuite delegate) {
            this.delegate = delegate;
        }

        /**
         * Selects integration-test mode from Kotlin DSL.
         *
         * @param build the named-build notation
         */
        @SuppressWarnings("unused") // Kotlin DSL invoke convention
        public void invoke(Object build) {
            delegate.forQuarkusIntegrationTests(build);
        }

        /**
         * Selects integration-test mode from Groovy DSL.
         *
         * @param build the named-build notation
         */
        @SuppressWarnings("unused") // Groovy DSL call convention
        public void call(Object build) {
            delegate.forQuarkusIntegrationTests(build);
        }
    }

    /**
     * Callable adapter installed as {@code startupArchiveTraining} on a JVM test suite.
     * <p>
     * It accepts a Gradle {@link Action} in Java or Kotlin and either an action or delegated closure in Groovy.
     */
    public static final class StartupArchiveTraining {

        private final QuarkusApplicationJvmTestSuite delegate;

        /**
         * Creates the callable adapter.
         *
         * @param delegate the owning Quarkus suite extension
         */
        public StartupArchiveTraining(QuarkusApplicationJvmTestSuite delegate) {
            this.delegate = delegate;
        }

        /**
         * Configures startup-archive training from Kotlin DSL.
         *
         * @param action the training configuration action
         */
        @SuppressWarnings("unused") // Kotlin DSL invoke convention
        public void invoke(Action<? super QuarkusApplicationStartupArchiveTraining> action) {
            delegate.startupArchiveTraining(action);
        }

        /**
         * Configures startup-archive training from Groovy DSL using a Gradle action.
         *
         * @param action the training configuration action
         */
        @SuppressWarnings("unused") // Groovy DSL call convention
        public void call(Action<? super QuarkusApplicationStartupArchiveTraining> action) {
            delegate.startupArchiveTraining(action);
        }

        /**
         * Configures startup-archive training from a Groovy delegated closure.
         * <p>
         * The training object is both the closure delegate and the positional argument, with delegate-first property
         * resolution.
         *
         * @param closure the training configuration closure
         */
        @SuppressWarnings("unused") // Groovy DSL call convention
        public void call(Closure<?> closure) {
            delegate.startupArchiveTraining(training -> {
                Closure<?> configured = closure.rehydrate(training, closure.getOwner(), closure.getThisObject());
                configured.setResolveStrategy(Closure.DELEGATE_FIRST);
                configured.call(training);
            });
        }
    }

    /**
     * Callable adapter installed as {@code includeTestsFrom} on a generated named native-test suite.
     * <p>
     * {@link #invoke(NamedDomainObjectProvider)} supports Kotlin invocation and
     * {@link #call(NamedDomainObjectProvider)} supports Groovy invocation.
     */
    public static final class IncludedTests {

        private final QuarkusApplicationJvmTestSuite delegate;

        /**
         * Creates the callable adapter.
         *
         * @param delegate the owning Quarkus suite extension
         */
        public IncludedTests(QuarkusApplicationJvmTestSuite delegate) {
            this.delegate = delegate;
        }

        /**
         * Includes tests from another suite using Kotlin DSL.
         *
         * @param includedSuite the project-owned JVM test suite to include
         */
        @SuppressWarnings("unused") // Kotlin DSL invoke convention
        public void invoke(NamedDomainObjectProvider<? extends JvmTestSuite> includedSuite) {
            delegate.includeTestsFrom(includedSuite);
        }

        /**
         * Includes tests from another suite using Groovy DSL.
         *
         * @param includedSuite the project-owned JVM test suite to include
         */
        @SuppressWarnings("unused") // Groovy DSL call convention
        public void call(NamedDomainObjectProvider<? extends JvmTestSuite> includedSuite) {
            delegate.includeTestsFrom(includedSuite);
        }

    }
}
