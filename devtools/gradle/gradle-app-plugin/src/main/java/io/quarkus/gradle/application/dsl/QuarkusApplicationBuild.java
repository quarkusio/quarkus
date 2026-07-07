package io.quarkus.gradle.application.dsl;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.jspecify.annotations.NonNull;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * Base DSL element for one named Quarkus application package or native output.
 * <p>
 * Register instances through {@link QuarkusApplicationBuilds}. The element's name and concrete subtype determine its
 * generated task names and immutable build type. Configuration is provider-backed: named-build properties override
 * matching root extension properties, while typed output-shape properties and native arguments are forced by the
 * corresponding operation. Merely registering a build does not execute it or attach it to {@code assemble}.
 */
public abstract class QuarkusApplicationBuild implements Named {

    private final String name;
    private final QuarkusApplicationBuildType buildType;
    private final QuarkusApplicationImage image;
    private final QuarkusApplicationDeployments deployments;
    private final QuarkusApplicationManifest manifest;

    /**
     * Creates the common state for a Gradle-managed named build.
     *
     * @param name the build name
     * @param buildType the fixed build type
     * @param objects Gradle's object factory
     * @param layout the project layout
     */
    protected QuarkusApplicationBuild(String name, QuarkusApplicationBuildType buildType, ObjectFactory objects,
            ProjectLayout layout) {
        this.name = requireNonNull(name, "name");
        this.buildType = requireNonNull(buildType, "buildType");
        this.image = objects.newInstance(QuarkusApplicationImage.class);
        this.deployments = objects.newInstance(QuarkusApplicationDeployments.class, objects);
        this.manifest = objects.newInstance(QuarkusApplicationManifest.class, objects);

        getOutputDirectory().convention(layout.getBuildDirectory().dir("quarkus-builds/" + name + "/package"));
        getParticipatesInAssemble().convention(false);
        getPrepareForOffline().convention(false);
        getQuarkusBuildProperties().convention(Map.of());
        getNativeArguments().convention(Map.of());
        image.getQuarkusBuildProperties().convention(Map.of());
    }

    /**
     * Returns the build name used to derive generated task and configuration names.
     *
     * @return the immutable build name
     */
    @Override
    public @NonNull String getName() {
        return name;
    }

    /**
     * Returns the package or native output type fixed by the concrete DSL subtype.
     *
     * @return the immutable build type
     */
    public QuarkusApplicationBuildType getBuildType() {
        return buildType;
    }

    /**
     * Returns the operation-owned output directory.
     * <p>
     * The convention is {@code build/quarkus-builds/<build-name>/package}.
     *
     * @return the lazily configurable output directory
     */
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Controls whether the standard Gradle {@code assemble} task depends on this build's package or native task.
     * <p>
     * The convention is {@code false}. Enabling it adds only the primary build task; image, push, deployment, run, and
     * test tasks remain outside the assemble graph.
     *
     * @return whether this named build participates in {@code assemble}
     */
    public abstract Property<Boolean> getParticipatesInAssemble();

    /**
     * Controls whether the aggregate {@code quarkusApplicationPrepareOffline} task includes this build's additional
     * dependency configurations.
     * <p>
     * The convention is {@code false}. Offline preparation resolves dependencies but does not execute this build or
     * side-effecting image, deployment, or test operations.
     *
     * @return whether offline preparation includes this named build
     */
    public abstract Property<Boolean> getPrepareForOffline();

    /**
     * Returns the base output name passed to Quarkus package generation.
     * <p>
     * Its lazy convention combines {@link #getArchiveBaseName()}, {@link #getArchiveBaseNameSuffix()}, and
     * {@link #getArchiveVersion()}. An explicitly set value bypasses that assembly and its
     * {@code project.version == "unspecified"} validation.
     *
     * @return the lazily configurable output name
     */
    public abstract Property<String> getOutputName();

    /**
     * Returns the first component of the conventional output name.
     * <p>
     * It conventionally uses {@code project.name} and must not be blank when the output-name convention is evaluated.
     *
     * @return the archive base name
     */
    public abstract Property<String> getArchiveBaseName();

    /**
     * Returns the optional suffix appended directly to the archive base name.
     * <p>
     * The convention is the empty string.
     *
     * @return the archive base-name suffix
     */
    public abstract Property<String> getArchiveBaseNameSuffix();

    /**
     * Returns the version appended to the conventional output name after a hyphen.
     * <p>
     * It conventionally uses {@code project.version}. A null or blank value omits the version; the literal
     * {@code unspecified} is rejected unless {@link #getOutputName()} is set explicitly.
     *
     * @return the archive version
     */
    public abstract Property<String> getArchiveVersion();

    /**
     * Returns Quarkus configuration applied to this named build after root extension properties.
     *
     * @return the lazily configurable build properties, empty by default
     */
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns native-operation configuration forced for this build's native executable or native-sources task.
     * <p>
     * These entries override matching generic build properties. The convention is an empty map.
     *
     * @return the lazily configurable native arguments
     */
    public abstract MapProperty<String, String> getNativeArguments();

    final QuarkusApplicationManifest manifest() {
        return manifest;
    }

    /**
     * Returns container-image configuration associated with this build.
     *
     * @return the image configuration
     */
    public QuarkusApplicationImage getImage() {
        return image;
    }

    /**
     * Configures container-image operations associated with this build.
     *
     * @param action the image configuration action
     */
    public void image(Action<? super QuarkusApplicationImage> action) {
        action.execute(image);
    }

    /**
     * Returns the named deployments associated with this build.
     *
     * @return the deployment container
     */
    public QuarkusApplicationDeployments getDeployments() {
        return deployments;
    }

    /**
     * Registers or configures deployments associated with this build.
     *
     * @param action the deployment-container configuration action
     */
    public void deployments(Action<? super QuarkusApplicationDeployments> action) {
        action.execute(deployments);
    }

}
