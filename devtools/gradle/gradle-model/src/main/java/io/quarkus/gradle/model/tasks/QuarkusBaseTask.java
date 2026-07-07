package io.quarkus.gradle.model.tasks;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Base class for Quarkus Gradle tasks that need normalized project identity and injected Gradle services.
 * <p>
 * The public type and service accessors support Gradle task decoration and subclasses in sibling plugin modules. This
 * is developer-facing plugin infrastructure, not an application build-script API.
 */
public abstract class QuarkusBaseTask extends DefaultTask {

    /**
     * Captures the project name, group, and initial description as declared task inputs.
     * <p>
     * The version is intentionally not captured because it may be configured after task construction.
     */
    protected QuarkusBaseTask() {
        getProjectName().set(getProject().getName());
        getProjectGroup().set(getProject().getGroup().toString());
        // Should NOT capture project-version here, because that can likely be configured
        // after this constructor runs.
        var description = getProject().getDescription();
        if (description != null) {
            getProjectDescription().set(description);
        }
    }

    /** @return the project name captured for task input tracking */
    @Input
    protected abstract Property<String> getProjectName();

    /** @return the optional project description captured for task input tracking */
    @Input
    @Optional
    protected abstract Property<String> getProjectDescription();

    /** @return the project group captured for task input tracking */
    @Input
    protected abstract Property<String> getProjectGroup();

    /** @return Gradle's injected provider factory */
    @Inject
    public abstract ProviderFactory getProviderFactory();

    /** @return Gradle's injected dependency handler */
    @Inject
    public abstract DependencyHandler getDependencyHandler();

    /** @return Gradle's injected object factory */
    @Inject
    public abstract ObjectFactory getObjects();
}
