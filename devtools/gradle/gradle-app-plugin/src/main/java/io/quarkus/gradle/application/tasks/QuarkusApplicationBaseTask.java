package io.quarkus.gradle.application.tasks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.model.tasks.TaskInputFingerprint;

/**
 * Implementation base for standalone-plugin tasks that consume explicitly selected ambient configuration.
 * <p>
 * This type is public so Gradle can decorate concrete task implementations. It is not a supported typed user entry
 * point and makes no compatibility commitment for direct construction, additional registration, or subclassing.
 * Plugin-created tasks normally receive selector conventions from the {@code quarkusApplication.configInputs} DSL.
 */
@DisableCachingByDefault(because = "Base application task has no standalone cacheable behavior")
public abstract class QuarkusApplicationBaseTask extends DefaultTask {

    static final String LEGACY_AMBIENT_CONFIG_CAPTURE_PROPERTY = "quarkusBuildLegacyAmbientConfigCapture";
    private static final String LEGACY_AMBIENT_CONFIG_CAPTURE_REASON = "legacy ambient config capture is enabled";

    /**
     * Initializes the task's build group and legacy ambient-capture policy.
     * <p>
     * The {@code quarkusBuildLegacyAmbientConfigCapture} Gradle property supplies a {@code false}-by-default convention.
     * Enabling it disables configuration-cache reuse, build caching, and up-to-date reuse for the task.
     */
    public QuarkusApplicationBaseTask() {
        setGroup("build");

        Provider<Boolean> legacyAmbientConfigCapture = getProviders()
                .gradleProperty(LEGACY_AMBIENT_CONFIG_CAPTURE_PROPERTY)
                .map(Boolean::parseBoolean)
                .orElse(false);
        getLegacyAmbientConfigCapture().convention(legacyAmbientConfigCapture);

        disableConfigurationCacheIfLegacyAmbientConfigCaptureEnabled();
        getOutputs().doNotCacheIf(LEGACY_AMBIENT_CONFIG_CAPTURE_REASON,
                task -> getLegacyAmbientConfigCapture().getOrElse(false));
        getOutputs().upToDateWhen(task -> !getLegacyAmbientConfigCapture().getOrElse(false));
    }

    /**
     * Returns whether the task captures every Gradle, system, and environment property instead of only declared
     * selectors.
     *
     * @return the legacy capture flag
     */
    @Internal
    public abstract Property<Boolean> getLegacyAmbientConfigCapture();

    /**
     * Returns prefixes selecting Gradle project properties.
     *
     * @return the declared project-property prefixes
     */
    @Input
    public abstract SetProperty<String> getGradlePropertyPrefixes();

    /**
     * Returns exact Gradle project-property names selected in addition to prefix matches.
     *
     * @return the declared project-property names
     */
    @Input
    public abstract SetProperty<String> getGradlePropertyNames();

    /**
     * Returns prefixes selecting JVM system properties.
     *
     * @return the declared system-property prefixes
     */
    @Input
    public abstract SetProperty<String> getSystemPropertyPrefixes();

    /**
     * Returns exact JVM system-property names selected in addition to prefix matches.
     *
     * @return the declared system-property names
     */
    @Input
    public abstract SetProperty<String> getSystemPropertyNames();

    /**
     * Returns prefixes selecting environment variables.
     *
     * @return the declared environment-variable prefixes
     */
    @Input
    public abstract SetProperty<String> getEnvironmentVariablePrefixes();

    /**
     * Returns exact environment-variable names selected in addition to prefix matches.
     *
     * @return the declared environment-variable names
     */
    @Input
    public abstract SetProperty<String> getEnvironmentVariableNames();

    /**
     * Returns Gradle's provider factory used to read selected ambient values lazily.
     *
     * @return the provider factory
     */
    @Inject
    protected abstract ProviderFactory getProviders();

    /**
     * Warns when compatibility capture has disabled normal Gradle reuse guarantees.
     */
    protected void warnIfLegacyAmbientConfigCaptureEnabled() {
        if (getLegacyAmbientConfigCapture().getOrElse(false)) {
            getLogger().warn("""
                    Legacy ambient config capture is enabled for Quarkus application tasks.
                    All environment variables, JVM system properties, and Gradle project properties may affect task execution.
                    Configuration-cache reuse, build caching, and up-to-date checks are disabled for these tasks.
                    Prefer declaring configInputs prefixes/names or quarkusBuildProperties.
                    """);
        }
    }

    private void disableConfigurationCacheIfLegacyAmbientConfigCaptureEnabled() {
        if (getLegacyAmbientConfigCapture().getOrElse(false)) {
            notCompatibleWithConfigurationCache(
                    "Legacy ambient config capture reads all Gradle properties, JVM system properties, and environment variables.");
        }
    }

    /**
     * Resolves selected Gradle project properties.
     * <p>
     * This is public only for Gradle task input modeling; it is not user configuration.
     *
     * @return the selected project-property values
     */
    @Internal
    public final Map<String, String> getSelectedGradleProperties() {
        if (getLegacyAmbientConfigCapture().getOrElse(false)) {
            return getProviders().gradlePropertiesPrefixedBy("").get();
        }
        return filteredMapEntries(
                getGradlePropertyPrefixes().get(),
                getGradlePropertyNames().get(),
                getProviders()::gradlePropertiesPrefixedBy,
                getProviders()::gradleProperty);
    }

    /**
     * Returns a deterministic task-input fingerprint of selected Gradle project properties.
     *
     * @return the selected project-property fingerprint
     */
    @Input
    public final String getSelectedGradlePropertiesFingerprint() {
        return TaskInputFingerprint.ofMap(getSelectedGradleProperties());
    }

    /**
     * Resolves selected environment variables.
     * <p>
     * This is public only for Gradle task input modeling; it is not user configuration.
     *
     * @return the selected environment-variable values
     */
    @Internal
    public final Map<String, String> getSelectedEnvironmentVariables() {
        if (getLegacyAmbientConfigCapture().getOrElse(false)) {
            return getProviders().environmentVariablesPrefixedBy("").get();
        }
        return filteredMapEntries(
                getEnvironmentVariablePrefixes().get(),
                getEnvironmentVariableNames().get(),
                getProviders()::environmentVariablesPrefixedBy,
                getProviders()::environmentVariable);
    }

    /**
     * Returns a deterministic task-input fingerprint of selected environment variables.
     *
     * @return the selected environment-variable fingerprint
     */
    @Input
    public final String getSelectedEnvironmentVariablesFingerprint() {
        return TaskInputFingerprint.ofMap(getSelectedEnvironmentVariables());
    }

    /**
     * Resolves selected JVM system properties.
     * <p>
     * This is public only for Gradle task input modeling; it is not user configuration.
     *
     * @return the selected system-property values
     */
    @Internal
    public final Map<String, String> getSelectedSystemProperties() {
        if (getLegacyAmbientConfigCapture().getOrElse(false)) {
            return getProviders().systemPropertiesPrefixedBy("").get();
        }
        return filteredMapEntries(
                getSystemPropertyPrefixes().get(),
                getSystemPropertyNames().get(),
                getProviders()::systemPropertiesPrefixedBy,
                getProviders()::systemProperty);
    }

    /**
     * Returns a deterministic task-input fingerprint of selected JVM system properties.
     *
     * @return the selected system-property fingerprint
     */
    @Input
    public final String getSelectedSystemPropertiesFingerprint() {
        return TaskInputFingerprint.ofMap(getSelectedSystemProperties());
    }

    Map<String, String> gradleProperties() {
        return getSelectedGradleProperties();
    }

    Map<String, String> environmentVariables() {
        return getSelectedEnvironmentVariables();
    }

    Map<String, String> systemProperties() {
        return getSelectedSystemProperties();
    }

    private Map<String, String> filteredMapEntries(Set<String> prefixes, Set<String> names,
            Function<String, Provider<Map<String, String>>> prefixedPropertiesProvider,
            Function<String, Provider<String>> propertyProvider) {
        var result = new LinkedHashMap<String, String>();
        for (var prefix : prefixes) {
            result.putAll(prefixedPropertiesProvider.apply(prefix).get());
        }
        for (var name : names) {
            String value = propertyProvider.apply(name).getOrNull();
            if (value != null) {
                result.put(name, value);
            }
        }
        return result;
    }
}
