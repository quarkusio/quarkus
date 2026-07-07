package io.quarkus.gradle.application.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.gradle.application.dsl.QuarkusApplicationForkOptions;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.ShapeExpectation;
import io.quarkus.gradle.application.internal.config.ShapeValidator;
import io.quarkus.gradle.application.internal.execution.BuildOperations;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.execution.worker.ForkOptionsSnapshot;
import io.quarkus.gradle.application.internal.execution.worker.WorkerBackedBuildOperations;
import io.quarkus.gradle.application.internal.planning.OutputLayoutPlanner;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;

/**
 * Implementation base for worker-backed package, native, image, deployment, and run operations.
 * <p>
 * Public visibility is required for Gradle decoration. Concrete plugin-created tasks own their task action and receipt;
 * this base owns modeled application inputs, output layout, effective-configuration validation, and worker isolation. It
 * is not a supported typed user entry point and makes no compatibility commitment for direct construction, additional
 * registration, or subclassing.
 */
@DisableCachingByDefault(because = "Quarkus application augmentation is not build-cacheable yet")
public abstract class QuarkusApplicationBuildTask extends QuarkusApplicationEffectiveConfigTask {

    /**
     * Captures the process {@code PATH} and optional {@code gradle.quarkus.gradle-worker.max-heap} system property for
     * worker setup.
     */
    public QuarkusApplicationBuildTask() {
        getPathEnvironment().set(getProviders().environmentVariable("PATH"));
        getGradleWorkerMaxHeap().set(getProviders().systemProperty("gradle.quarkus.gradle-worker.max-heap"));
    }

    /**
     * Returns Gradle's build directory used to allocate operation-local intermediate state.
     *
     * @return the Gradle build directory
     */
    @Internal
    public abstract DirectoryProperty getGradleBuildDirectory();

    /**
     * Returns the complete output directory owned by the concrete operation.
     *
     * @return the operation output directory
     */
    @Override
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Returns the serialized application model consumed by augmentation.
     *
     * @return the application-model file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    /**
     * Returns the runtime and augmentation classpath.
     *
     * @return the runtime classpath
     */
    @Classpath
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    /**
     * Returns an injected operation implementation when tests or plugin wiring provide one.
     *
     * @return the optional operation implementation
     */
    @Internal
    protected abstract Property<BuildOperations> getOperations();

    /**
     * Returns worker JVM isolation options copied from the root extension.
     *
     * @return the build-worker fork options
     */
    @Nested
    public abstract QuarkusApplicationForkOptions getBuildForkOptions();

    /**
     * Returns the captured process {@code PATH} used to set up the worker environment.
     *
     * @return the path environment
     */
    @Internal
    protected abstract Property<String> getPathEnvironment();

    /**
     * Returns the optional maximum heap override for Gradle's Quarkus worker.
     *
     * @return the worker maximum heap setting
     */
    @Internal
    protected abstract Property<String> getGradleWorkerMaxHeap();

    /**
     * Returns Gradle's worker executor.
     *
     * @return the worker executor
     */
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    /**
     * Creates a validated immutable request for the concrete operation.
     *
     * @param operationForcedProperties operation-specific properties with final precedence
     * @return the build request
     */
    protected BuildRequest buildRequest(Map<String, String> operationForcedProperties) {
        warnIfLegacyAmbientConfigCaptureEnabled();
        QuarkusApplicationBuildDescriptor descriptor = descriptor();
        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        var layout = new OutputLayoutPlanner().plan(
                getGradleBuildDirectory().get().getAsFile().toPath(), descriptor, outputRoot);
        EffectiveConfigPlan effectiveConfig = effectiveConfig(operationForcedProperties);
        new ShapeValidator().validate(new ShapeExpectation(
                getBuildName().get(), getPath(), effectiveConfig.descriptorShapeValues()), effectiveConfig.fullValues());
        return new BuildRequest(
                descriptor,
                outputRoot,
                getApplicationModel().get().getAsFile().toPath(),
                getRuntimeClasspath().getFiles().stream().map(File::toPath).toList(),
                getSourceDirectories().getFiles(),
                effectiveConfig,
                effectiveConfig.buildSystemProperties(),
                operationForcedProperties,
                true,
                layout);
    }

    private QuarkusApplicationBuildDescriptor descriptor() {
        return QuarkusApplicationBuildDescriptor.of(getBuildName().get(), getBuildType().get());
    }

    /**
     * Returns the configured build operations or creates the normal worker-backed implementation.
     *
     * @return the build operations
     */
    protected BuildOperations buildOperations() {
        BuildOperations configured = getOperations().getOrNull();
        if (configured != null) {
            return configured;
        }
        return new WorkerBackedBuildOperations(
                getWorkerExecutor(),
                getProviders(),
                ForkOptionsSnapshot.from(getBuildForkOptions()),
                getPathEnvironment().getOrNull(),
                getGradleWorkerMaxHeap().getOrNull());
    }
}
