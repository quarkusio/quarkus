package io.quarkus.gradle.application.tasks;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.gradle.application.dsl.QuarkusApplicationForkOptions;
import io.quarkus.gradle.application.internal.codegen.CodegenOperations;
import io.quarkus.gradle.application.internal.codegen.CodegenRequest;
import io.quarkus.gradle.application.internal.codegen.worker.WorkerBackedCodegenOperations;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlanner;
import io.quarkus.gradle.application.internal.config.EffectiveConfigRequest;
import io.quarkus.gradle.application.internal.execution.worker.ForkOptionsSnapshot;
import io.quarkus.runtime.LaunchMode;

/*
 * The generic provider contract permits ambient process state and writes to the shared build directory, so
 * it cannot define a complete task-level cache contract. Some provider outputs may still be cacheable on
 * their own. Revisit caching per provider only after its inputs, local state, auxiliary outputs, and generated
 * artifacts have a complete Gradle model. That may require provider-specific tasks or a richer provider contract.
 */
/**
 * Plugin-created implementation task for Quarkus main, development, and test source generation.
 * <p>
 * Public visibility is required for Gradle decoration. The plugin owns its registered instances, launch mode,
 * application model, classpaths, source discovery, output directory, and invocation order. This type is not a supported
 * typed user entry point and makes no compatibility commitment for direct construction, additional registration, or
 * subclassing. It is deliberately not build-cacheable because the generic provider contract permits unmodeled process
 * state and auxiliary outputs.
 */
@DisableCachingByDefault(because = "Provider process state and auxiliary outputs are unmodeled; the application model is not relocatable")
public abstract class QuarkusApplicationGenerateCodeTask extends QuarkusApplicationBaseTask {

    /**
     * Captures worker environment conventions.
     */
    public QuarkusApplicationGenerateCodeTask() {
        getPathEnvironment().set(getProviders().environmentVariable("PATH"));
        getGradleWorkerMaxHeap().set(getProviders().systemProperty("gradle.quarkus.gradle-worker.max-heap"));
    }

    /**
     * Returns the launch mode whose profile and application model govern generation.
     *
     * @return the required launch mode
     */
    @Input
    public abstract Property<LaunchMode> getLaunchMode();

    /**
     * Returns effective Quarkus configuration supplied to code-generation providers.
     *
     * @return the configured build properties
     */
    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns the application name supplied to provider bootstrap.
     *
     * @return the application name
     */
    @Input
    public abstract Property<String> getApplicationName();

    /**
     * Returns the application version supplied to effective configuration.
     *
     * @return the application version
     */
    @Input
    public abstract Property<String> getApplicationVersion();

    /**
     * Returns provider identifiers searched beneath source roots.
     *
     * @return the configured provider identifiers
     */
    @Input
    public abstract ListProperty<String> getCodegenProviders();

    /**
     * Returns input-directory names searched beneath source roots.
     *
     * @return the configured input-directory names
     */
    @Input
    public abstract ListProperty<String> getCodegenInputNames();

    /**
     * Returns the serialized application model for the selected launch mode.
     *
     * @return the application-model file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    /**
     * Returns the worker compile classpath.
     *
     * @return the code-generation worker classpath
     */
    @CompileClasspath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Runtime dependency artifacts are separate from the worker classpath because code generators may consume non-class
     * resources from them. {@link CompileClasspath} normalization intentionally ignores those resources.
     *
     * @return the dependency artifacts visible to providers
     */
    @Classpath
    public abstract ConfigurableFileCollection getCodegenDependencyArtifacts();

    /**
     * Returns parent directories containing provider-specific input directories.
     *
     * @return the source-parent directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceParentDirectories();

    /**
     * Resource source directories make application configuration available before {@code processResources} has run.
     *
     * @return the configuration source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getConfigurationSourceDirectories();

    /**
     * Returns the generated-sources directory owned by this task.
     *
     * @return the generated output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getGeneratedOutputDirectory();

    /**
     * Returns the shared Gradle build directory available to provider implementations for auxiliary state.
     *
     * @return the build directory
     */
    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    /**
     * Returns code-generation worker JVM options.
     *
     * @return the worker fork options
     */
    @Nested
    public abstract QuarkusApplicationForkOptions getCodegenForkOptions();

    /**
     * Returns the captured process {@code PATH}.
     *
     * @return the path environment
     */
    @Internal
    protected abstract Property<String> getPathEnvironment();

    /**
     * Returns the optional Gradle Quarkus worker maximum heap setting.
     *
     * @return the worker maximum heap setting
     */
    @Internal
    protected abstract Property<String> getGradleWorkerMaxHeap();

    /**
     * Returns an injected code-generation implementation for plugin tests.
     *
     * @return the optional operation implementation
     */
    @Internal // Only for testing purposes
    protected abstract Property<CodegenOperations> getOperations();

    /**
     * Returns Gradle's worker executor.
     *
     * @return the worker executor
     */
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    /**
     * Resolves effective inputs and invokes the selected code-generation providers.
     */
    @TaskAction
    public void generateCode() {
        CodegenRequest request = codegenRequest();
        codegenOperations().generate(request);
    }

    CodegenRequest codegenRequest() {
        warnIfLegacyAmbientConfigCaptureEnabled();
        EffectiveConfigPlan effectiveConfig = effectiveConfig();
        return new CodegenRequest(
                getApplicationModel().get().getAsFile().toPath(),
                getLaunchMode().get(),
                getSourceParentDirectories().getFiles(),
                getGeneratedOutputDirectory().get().getAsFile().toPath(),
                getBuildDirectory().get().getAsFile().toPath(),
                getApplicationName().get(),
                getCodegenProviders().get(),
                getCodegenInputNames().get(),
                getClasspath().getFiles().stream().map(File::toPath).toList(),
                effectiveConfig,
                codegenBuildSystemProperties(effectiveConfig));
    }

    private static Map<String, String> codegenBuildSystemProperties(EffectiveConfigPlan effectiveConfig) {
        Map<String, String> buildSystemProperties = new LinkedHashMap<>(effectiveConfig.buildSystemProperties());
        // Configuration-file values normally remain available from application resources instead of being propagated to
        // a worker. The pre-resource codegen model has no application resource output on its paths, so providers need the
        // final effective Quarkus values in their bootstrap properties. These are not used as forked JVM system properties.
        effectiveConfig.fullValues().forEach((name, value) -> {
            if (name.startsWith("quarkus.") || name.startsWith("platform.quarkus.")) {
                buildSystemProperties.put(name, value);
            }
        });
        return buildSystemProperties;
    }

    private EffectiveConfigPlan effectiveConfig() {
        EffectiveConfigPlan plan = new EffectiveConfigPlanner().plan(
                new EffectiveConfigRequest(
                        Map.of(),
                        getApplicationName().get(),
                        getApplicationVersion().get(),
                        getConfigurationSourceDirectories().getFiles(),
                        getQuarkusBuildProperties().get(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        gradleProperties(),
                        environmentVariables(),
                        systemProperties(),
                        Map.of(),
                        getLaunchMode().get().getDefaultProfile()));
        if (!getLegacyAmbientConfigCapture().getOrElse(false)) {
            return plan;
        }
        return new EffectiveConfigPlan(
                plan.fullValues(),
                plan.quarkusWorkerValues(),
                plan.fullValues(),
                plan.descriptorShapeValues(),
                plan.diagnostics(),
                plan.externallyProvidedValuesOmitted(),
                plan.configSourceNames());
    }

    private CodegenOperations codegenOperations() {
        CodegenOperations configured = getOperations().getOrNull();
        if (configured != null) {
            return configured;
        }
        return new WorkerBackedCodegenOperations(
                getWorkerExecutor(),
                getProviders(),
                ForkOptionsSnapshot.from(getCodegenForkOptions()),
                getPathEnvironment().getOrNull(),
                getGradleWorkerMaxHeap().getOrNull());
    }
}
