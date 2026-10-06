package io.quarkus.gradle.application.dsl;

import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;

/**
 * Declares which project properties, system properties, and environment variables may influence Quarkus application
 * tasks.
 * <p>
 * Explicit selection lets Gradle fingerprint configuration without capturing the entire ambient process environment.
 * Project and system properties conventionally include keys beginning with {@code quarkus.}, {@code platform.quarkus.},
 * or {@code smallrye.config.}; environment variables conventionally include {@code QUARKUS_},
 * {@code PLATFORM_QUARKUS_}, or {@code SMALLRYE_CONFIG_}. Exact-name sets are empty by default.
 */
public abstract class QuarkusApplicationConfigInputs {

    private final QuarkusApplicationConfigInputSet projectProperties;
    private final QuarkusApplicationConfigInputSet systemProperties;
    private final QuarkusApplicationConfigInputSet environmentVariables;

    /**
     * Creates the three source-specific selectors and their standard conventions.
     *
     * @param objects Gradle's object factory
     * @param providers Gradle's provider factory
     */
    @Inject
    public QuarkusApplicationConfigInputs(ObjectFactory objects, ProviderFactory providers) {
        this.projectProperties = objects.newInstance(QuarkusApplicationConfigInputSet.class);
        this.systemProperties = objects.newInstance(QuarkusApplicationConfigInputSet.class);
        this.environmentVariables = objects.newInstance(QuarkusApplicationConfigInputSet.class);

        projectProperties.getPrefixes().convention(Set.of("quarkus.", "platform.quarkus.", "smallrye.config."));
        projectProperties.getNames().convention(Set.of());
        systemProperties.getPrefixes().convention(Set.of("quarkus.", "platform.quarkus.", "smallrye.config."));
        systemProperties.getNames().convention(Set.of());
        environmentVariables.getPrefixes().convention(Set.of("QUARKUS_", "PLATFORM_QUARKUS_", "SMALLRYE_CONFIG_"));
        environmentVariables.getNames().convention(Set.of());
        getLegacyAmbientConfigCapture().convention(
                providers.gradleProperty("quarkusBuildLegacyAmbientConfigCapture")
                        .map(Boolean::parseBoolean)
                        .orElse(false));
    }

    /**
     * Returns the selector for Gradle project properties.
     *
     * @return the project-property selector
     */
    public QuarkusApplicationConfigInputSet getProjectProperties() {
        return projectProperties;
    }

    /**
     * Configures the project-property selector.
     *
     * @param action the lazy-property configuration action
     */
    public void projectProperties(Action<? super QuarkusApplicationConfigInputSet> action) {
        action.execute(projectProperties);
    }

    /**
     * Returns the selector for JVM system properties.
     *
     * @return the system-property selector
     */
    public QuarkusApplicationConfigInputSet getSystemProperties() {
        return systemProperties;
    }

    /**
     * Configures the system-property selector.
     *
     * @param action the lazy-property configuration action
     */
    public void systemProperties(Action<? super QuarkusApplicationConfigInputSet> action) {
        action.execute(systemProperties);
    }

    /**
     * Returns the selector for process environment variables.
     *
     * @return the environment-variable selector
     */
    public QuarkusApplicationConfigInputSet getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * Configures the environment-variable selector.
     *
     * @param action the lazy-property configuration action
     */
    public void environmentVariables(Action<? super QuarkusApplicationConfigInputSet> action) {
        action.execute(environmentVariables);
    }

    /**
     * Enables compatibility capture of ambient configuration beyond the explicit selectors.
     * <p>
     * The convention is read lazily from the {@code quarkusBuildLegacyAmbientConfigCapture} Gradle property and is
     * {@code false} when absent. Enabling it makes affected tasks incompatible with normal caching and up-to-date
     * behavior because undeclared ambient values may participate.
     *
     * @return whether legacy ambient capture is enabled
     */
    public abstract Property<Boolean> getLegacyAmbientConfigCapture();
}
