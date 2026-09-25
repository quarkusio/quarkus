package io.quarkus.gradle.application.dsl;

import java.time.Instant;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;

/**
 * Root DSL for the standalone {@code io.quarkus.application} Gradle plugin, available as
 * {@code quarkusApplication}.
 * <p>
 * The extension owns provider-backed configuration for named builds, explicit ambient configuration inputs, code
 * generation, dev and remote-dev launches, finite tests, and build-worker isolation. Named builds register their task
 * families lazily, while test-suite opt-ins lazily configure matching test targets; configuring this root alone does not
 * execute work. Root Quarkus build properties provide the common baseline and concern- or named-build-specific
 * properties override matching entries.
 */
public abstract class QuarkusApplicationExtension {

    private static final Instant DEFAULT_PACKAGE_OUTPUT_TIMESTAMP = Instant.parse("1970-01-02T00:00:00Z");

    private final QuarkusApplicationBuilds builds;
    private final QuarkusApplicationConfigInputs configInputs;
    private final QuarkusApplicationCodegen codegen;
    private final QuarkusApplicationDev dev;
    private final QuarkusApplicationRemoteDev remoteDev;
    private final QuarkusApplicationTests tests;
    private final QuarkusApplicationForkOptions buildForkOptions;
    private final QuarkusApplicationForkOptions codeGenForkOptions;

    /**
     * Creates the Gradle-managed root extension and its nested DSL blocks.
     *
     * @param objects Gradle's object factory
     * @param providers Gradle's provider factory
     * @param layout the project layout
     * @param projectName the project name used by named-build conventions
     * @param projectVersion the lazy project version used by named-build conventions
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     */
    @Inject
    public QuarkusApplicationExtension(ObjectFactory objects, ProviderFactory providers, ProjectLayout layout,
            String projectName, Provider<String> projectVersion, Object lifecycle) {
        DslLifecycleCoordinator coordinator = requireLifecycle(lifecycle);
        this.builds = objects.newInstance(QuarkusApplicationBuilds.class, objects, providers, layout, projectName,
                projectVersion, coordinator);
        this.configInputs = objects.newInstance(QuarkusApplicationConfigInputs.class, objects, providers);
        this.codegen = objects.newInstance(QuarkusApplicationCodegen.class);
        this.dev = objects.newInstance(QuarkusApplicationDev.class);
        this.remoteDev = objects.newInstance(QuarkusApplicationRemoteDev.class);
        this.tests = objects.newInstance(QuarkusApplicationTests.class, coordinator);
        this.buildForkOptions = objects.newInstance(QuarkusApplicationForkOptions.class);
        this.codeGenForkOptions = objects.newInstance(QuarkusApplicationForkOptions.class);
        getQuarkusBuildProperties().convention(Map.of());
        getPackageOutputTimestamp().convention(DEFAULT_PACKAGE_OUTPUT_TIMESTAMP);
    }

    private static DslLifecycleCoordinator requireLifecycle(Object lifecycle) {
        if (lifecycle instanceof DslLifecycleCoordinator coordinator) {
            return coordinator;
        }
        throw new IllegalArgumentException("Quarkus application extension requires its internal lifecycle coordinator");
    }

    /**
     * Returns common Quarkus configuration for application operations.
     * <p>
     * The map is empty by default. Named-build, image, dev, or remote-dev properties override matching root entries;
     * typed operation-owned settings and command-line task options have their documented higher precedence.
     *
     * @return the lazily configurable common build properties
     */
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * The reference timestamp used for generated package entries. The default produces deterministic package timestamps
     * without coupling named application builds to Gradle's {@code jar} task. Call {@link Property#unsetConvention()} to
     * leave package entry timestamps unnormalized. An explicit {@code quarkus.package.output-timestamp} build property,
     * and normal higher-precedence task, project, system, or operation-forced configuration, override this convention.
     *
     * @return the package output timestamp
     */
    public abstract Property<Instant> getPackageOutputTimestamp();

    /**
     * Returns JVM fork options shared by application build, native, image, and deployment workers.
     *
     * @return the build-worker fork options
     */
    public QuarkusApplicationForkOptions getBuildForkOptions() {
        return buildForkOptions;
    }

    /**
     * Returns JVM fork options shared by code-generation workers.
     *
     * @return the code-generation worker fork options
     */
    public QuarkusApplicationForkOptions getCodeGenForkOptions() {
        return codeGenForkOptions;
    }

    /**
     * Configures JVM isolation for application build workers.
     *
     * @param action the fork-options configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void buildForkOptions(Action<? super QuarkusApplicationForkOptions> action) {
        action.execute(buildForkOptions);
    }

    /**
     * Configures JVM isolation for code-generation workers.
     *
     * @param action the fork-options configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void codeGenForkOptions(Action<? super QuarkusApplicationForkOptions> action) {
        action.execute(codeGenForkOptions);
    }

    /**
     * Returns the named application-build container.
     *
     * @return the named-build DSL
     */
    public QuarkusApplicationBuilds getBuilds() {
        return builds;
    }

    /**
     * Registers or configures named application builds.
     *
     * @param action the named-build configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void builds(Action<? super QuarkusApplicationBuilds> action) {
        action.execute(builds);
    }

    /**
     * Returns explicit ambient configuration-input selectors.
     *
     * @return the configuration-input DSL
     */
    public QuarkusApplicationConfigInputs getConfigInputs() {
        return configInputs;
    }

    /**
     * Configures explicit ambient configuration-input selectors.
     *
     * @param action the input-selector configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void configInputs(Action<? super QuarkusApplicationConfigInputs> action) {
        action.execute(configInputs);
    }

    /**
     * Returns code-generation source-discovery configuration.
     *
     * @return the code-generation DSL
     */
    public QuarkusApplicationCodegen getCodegen() {
        return codegen;
    }

    /**
     * Configures code-generation source discovery.
     *
     * @param action the code-generation configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void codegen(Action<? super QuarkusApplicationCodegen> action) {
        action.execute(codegen);
    }

    /**
     * Returns local dev-mode launch configuration.
     *
     * @return the dev-mode DSL
     */
    public QuarkusApplicationDev getDev() {
        return dev;
    }

    /**
     * Configures local dev-mode and dedicated continuous-test launches.
     *
     * @param action the dev-mode configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void dev(Action<? super QuarkusApplicationDev> action) {
        action.execute(dev);
    }

    /**
     * Returns remote-dev build and launch configuration.
     *
     * @return the remote-dev DSL
     */
    public QuarkusApplicationRemoteDev getRemoteDev() {
        return remoteDev;
    }

    /**
     * Configures standalone remote-dev operations.
     *
     * @param action the remote-dev configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void remoteDev(Action<? super QuarkusApplicationRemoteDev> action) {
        action.execute(remoteDev);
    }

    /**
     * Returns finite Gradle {@link org.gradle.api.tasks.testing.Test} task selection.
     *
     * @return the finite-test selection DSL
     */
    public QuarkusApplicationTests getTests() {
        return tests;
    }

    /**
     * Selects finite Gradle test tasks for Quarkus test setup.
     *
     * @param action the finite-test selection action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void tests(Action<? super QuarkusApplicationTests> action) {
        action.execute(tests);
    }
}
