package io.quarkus.gradle.application.tasks;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlanner;
import io.quarkus.gradle.application.internal.config.EffectiveConfigRequest;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * Implementation base that assembles effective Quarkus configuration for named application build operations.
 * <p>
 * Public visibility is required for Gradle decoration. The plugin wires root and named-build properties, typed
 * descriptor-shape constraints, manifest properties, selected ambient values, and operation-forced properties through
 * this type. It is not a supported typed user entry point and makes no compatibility commitment for direct
 * construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Base effective-configuration task has no standalone cacheable behavior")
public abstract class QuarkusApplicationEffectiveConfigTask extends QuarkusApplicationTask {

    private static final String BUILD_PROFILE = "prod";
    private static final String PACKAGE_OUTPUT_TIMESTAMP = "quarkus.package.output-timestamp";

    /**
     * Initializes empty typed descriptor-shape and manifest-property conventions.
     */
    public QuarkusApplicationEffectiveConfigTask() {
        getAdditionalDescriptorShapeProperties().convention(Map.of());
        getManifestConfigProperties().convention(Map.of());
    }

    /**
     * Returns root plus named-operation Quarkus properties.
     *
     * @return the configured build properties
     */
    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns the optional deterministic package-entry timestamp convention.
     * <p>
     * An explicit {@code quarkus.package.output-timestamp} build property overrides this value.
     *
     * @return the package output timestamp
     */
    @Input
    @Optional
    public abstract Property<Instant> getPackageOutputTimestamp();

    /**
     * Returns additional typed descriptor-shape values forced for the operation, such as runner naming.
     *
     * @return the additional forced descriptor-shape properties
     */
    @Input
    public abstract MapProperty<String, String> getAdditionalDescriptorShapeProperties();

    /**
     * Returns fully expanded Quarkus configuration properties produced from the named JAR output's manifest DSL.
     * These values override conflicting generic build properties.
     *
     * @return the expanded manifest properties
     */
    @Input
    public abstract MapProperty<String, String> getManifestConfigProperties();

    /**
     * Returns the application name used by effective configuration.
     *
     * @return the application name
     */
    @Input
    public abstract Property<String> getApplicationName();

    /**
     * Returns the application version used by effective configuration.
     *
     * @return the application version
     */
    @Input
    public abstract Property<String> getApplicationVersion();

    /**
     * Returns source and resource directories inspected for application configuration.
     *
     * @return the configuration source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirectories();

    /**
     * Builds the production-profile effective configuration plan.
     * <p>
     * Operation-forced values and typed descriptor-shape values have final precedence.
     *
     * @param operationForcedProperties operation-specific forced values
     * @return the effective configuration plan
     */
    protected final EffectiveConfigPlan effectiveConfig(Map<String, String> operationForcedProperties) {
        Map<String, String> forced = new LinkedHashMap<>(operationForcedProperties);
        forced.putAll(descriptorShapeProperties());
        EffectiveConfigPlan plan = new EffectiveConfigPlanner().plan(
                new EffectiveConfigRequest(
                        Map.of(),
                        getApplicationName().get(),
                        getApplicationVersion().get(),
                        getSourceDirectories().getFiles(),
                        quarkusBuildPropertiesWithPackageOutputTimestamp(),
                        Map.of(),
                        forced,
                        getManifestConfigProperties().get(),
                        gradleProperties(),
                        environmentVariables(),
                        systemProperties(),
                        Map.of(),
                        effectiveConfigProfile()));
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

    /**
     * Returns the fixed build profile used by package-oriented operations.
     *
     * @return {@code prod}
     */
    protected final String effectiveConfigProfile() {
        return BUILD_PROFILE;
    }

    private Map<String, String> descriptorShapeProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        QuarkusApplicationBuildType type = getBuildType().get();
        properties.put("quarkus.package.output-directory", getOutputDirectory().get().getAsFile().toPath().toString());
        properties.put("quarkus.package.output-name", getOutputName().getOrElse(getBuildName().get()));
        properties.put("quarkus.package.jar.enabled", Boolean.toString(type.isJar()));
        properties.put("quarkus.native.enabled", Boolean.toString(type.isNativeOutput()));
        if (type.isNativeOutput()) {
            properties.put("quarkus.native.sources-only", Boolean.toString(type.isNativeSources()));
        }
        type.jarType().ifPresent(jarType -> properties.put("quarkus.package.jar.type", jarType));
        properties.putAll(getAdditionalDescriptorShapeProperties().get());
        return properties;
    }

    private Map<String, String> quarkusBuildPropertiesWithPackageOutputTimestamp() {
        Map<String, String> buildProperties = new LinkedHashMap<>();
        Instant packageOutputTimestamp = getPackageOutputTimestamp().getOrNull();
        if (packageOutputTimestamp != null) {
            buildProperties.put(PACKAGE_OUTPUT_TIMESTAMP, packageOutputTimestamp.toString());
        }
        buildProperties.putAll(getQuarkusBuildProperties().get());
        return buildProperties;
    }
}
