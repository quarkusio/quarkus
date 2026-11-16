package io.quarkus.gradle.model.tasks;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public abstract class QuarkusBaseTask extends DefaultTask {

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

    @Input
    protected abstract Property<String> getProjectName();

    @Input
    @Optional
    protected abstract Property<String> getProjectDescription();

    @Input
    protected abstract Property<String> getProjectGroup();

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @Inject
    public abstract DependencyHandler getDependencyHandler();

    @Inject
    public abstract ObjectFactory getObjects();
}
