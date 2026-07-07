package io.quarkus.extension.gradle;

import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import io.quarkus.extension.gradle.dsl.Capabilities;
import io.quarkus.extension.gradle.dsl.Capability;
import io.quarkus.extension.gradle.dsl.RemovedResource;
import io.quarkus.extension.gradle.dsl.RemovedResources;

/**
 * Configuration for the {@code io.quarkus.extension} Gradle plugin.
 * <p>
 * The plugin creates this extension as {@code quarkusExtension}. It describes the deployment artifact or local
 * deployment module, extension dependency and class-loading metadata, capabilities, and removed resources used to
 * generate {@code META-INF/quarkus-extension.properties} and {@code META-INF/quarkus-extension.yaml}.
 * <p>
 * Gradle-managed {@link Property} and {@link ListProperty} values remain lazy until the plugin's tasks and outgoing
 * variants consume them. The legacy-style list setters append their values rather than replacing earlier declarations.
 */
public class QuarkusExtensionConfiguration {

    private Property<Boolean> disableValidation;
    private Property<String> deploymentArtifact;
    private Property<String> deploymentModule;
    private ListProperty<String> excludedArtifacts;
    private ListProperty<String> parentFirstArtifacts;
    private ListProperty<String> runnerParentFirstArtifacts;
    private ListProperty<String> lesserPriorityArtifacts;
    private ListProperty<String> conditionalDependencies;
    private ListProperty<String> conditionalDevDependencies;
    private ListProperty<String> dependencyCondition;
    private RemovedResources removedResources = new RemovedResources();
    private Capabilities capabilities = new Capabilities();

    /**
     * Creates the extension configuration and its Gradle-managed properties.
     *
     * @param objects Gradle's object factory
     */
    public QuarkusExtensionConfiguration(ObjectFactory objects) {
        disableValidation = objects.property(Boolean.class);
        disableValidation.convention(false);
        deploymentArtifact = objects.property(String.class);
        deploymentModule = objects.property(String.class);
        deploymentModule.convention("deployment");

        excludedArtifacts = objects.listProperty(String.class);
        parentFirstArtifacts = objects.listProperty(String.class);
        runnerParentFirstArtifacts = objects.listProperty(String.class);
        lesserPriorityArtifacts = objects.listProperty(String.class);
        conditionalDependencies = objects.listProperty(String.class);
        conditionalDevDependencies = objects.listProperty(String.class);
        dependencyCondition = objects.listProperty(String.class);
    }

    /**
     * Sets whether extension dependency validation is skipped.
     * <p>
     * This is the Groovy/property-style {@code disableValidation} entry point. Validation is enabled by default.
     *
     * @param disableValidation whether to skip validation
     */
    public void setDisableValidation(boolean disableValidation) {
        this.disableValidation.set(disableValidation);
    }

    /**
     * Returns the provider used by plugin tasks to decide whether extension dependency validation is disabled.
     *
     * @return whether validation is disabled; the convention is {@code false}
     */
    public Property<Boolean> isValidationDisabled() {
        return disableValidation;
    }

    /**
     * Returns an explicitly published deployment artifact coordinate.
     * <p>
     * When absent, the descriptor task derives the coordinate from the runtime project's group, name, and version using
     * the conventional {@code -deployment} artifact suffix.
     *
     * @return the optional deployment artifact coordinate
     */
    public Property<String> getDeploymentArtifact() {
        return deploymentArtifact;
    }

    /**
     * Sets the published deployment artifact coordinate.
     *
     * @param deploymentArtifact the deployment artifact coordinate
     */
    public void setDeploymentArtifact(String deploymentArtifact) {
        this.deploymentArtifact.set(deploymentArtifact);
    }

    /**
     * Returns the local deployment project name or absolute Gradle project path.
     *
     * @return the deployment module; the convention is {@code deployment}
     */
    public Property<String> getDeploymentModule() {
        return deploymentModule;
    }

    /**
     * Sets the local deployment project name or absolute Gradle project path.
     *
     * @param deploymentModule the deployment module
     */
    public void setDeploymentModule(String deploymentModule) {
        this.deploymentModule.set(deploymentModule);
    }

    /**
     * Returns artifacts excluded from the application dependency model when this extension is present.
     *
     * @return the excluded artifact declarations
     */
    public ListProperty<String> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    /**
     * Appends artifacts excluded from the application dependency model.
     *
     * @param excludedArtifacts artifact declarations to append
     */
    public void setExcludedArtifacts(List<String> excludedArtifacts) {
        this.excludedArtifacts.addAll(excludedArtifacts);
    }

    /**
     * Returns artifacts loaded parent-first in all application launch modes.
     *
     * @return the parent-first artifact declarations
     */
    public ListProperty<String> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    /**
     * Appends artifacts loaded parent-first in all application launch modes.
     *
     * @param parentFirstArtifacts artifact declarations to append
     */
    public void setParentFirstArtifacts(List<String> parentFirstArtifacts) {
        this.parentFirstArtifacts.addAll(parentFirstArtifacts);
    }

    /**
     * Returns artifacts loaded parent-first only by application runners.
     *
     * @return the runner parent-first artifact declarations
     */
    public ListProperty<String> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    /**
     * Appends artifacts loaded parent-first only by application runners.
     *
     * @param runnerParentFirstArtifacts artifact declarations to append
     */
    public void setRunnerParentFirstArtifacts(List<String> runnerParentFirstArtifacts) {
        this.runnerParentFirstArtifacts.addAll(runnerParentFirstArtifacts);
    }

    /**
     * Returns artifacts assigned lesser class-loading priority.
     *
     * @return the lesser-priority artifact declarations
     */
    public ListProperty<String> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    /**
     * Appends artifacts assigned lesser class-loading priority.
     *
     * @param lesserPriorityArtifacts artifact declarations to append
     */
    public void setLesserPriorityArtifacts(List<String> lesserPriorityArtifacts) {
        this.lesserPriorityArtifacts.addAll(lesserPriorityArtifacts);
    }

    /**
     * Returns extension dependencies activated when their dependency conditions are satisfied.
     *
     * @return the conditional dependency coordinates
     */
    public ListProperty<String> getConditionalDependencies() {
        return conditionalDependencies;
    }

    /**
     * Appends extension dependencies activated when their dependency conditions are satisfied.
     *
     * @param conditionalDependencies dependency coordinates to append
     */
    public void setConditionalDependencies(List<String> conditionalDependencies) {
        this.conditionalDependencies.addAll(conditionalDependencies);
    }

    /**
     * Returns development-only extension dependencies activated when their dependency conditions are satisfied.
     *
     * @return the conditional development dependency coordinates
     */
    public ListProperty<String> getConditionalDevDependencies() {
        return conditionalDevDependencies;
    }

    /**
     * Appends development-only extension dependencies activated when their dependency conditions are satisfied.
     *
     * @param conditionalDependencies dependency coordinates to append
     */
    public void setConditionalDevDependencies(List<String> conditionalDependencies) {
        this.conditionalDevDependencies.addAll(conditionalDependencies);
    }

    /**
     * Returns artifact conditions that control activation of this extension as a conditional dependency.
     *
     * @return the dependency-condition artifact declarations
     */
    public ListProperty<String> getDependencyConditions() {
        return dependencyCondition;
    }

    /**
     * Appends artifact conditions that control activation of this extension as a conditional dependency.
     *
     * @param dependencyCondition artifact declarations to append
     */
    public void setDependencyConditions(List<String> dependencyCondition) {
        this.dependencyCondition.addAll(dependencyCondition);
    }

    /**
     * Returns capabilities provided by this extension.
     *
     * @return provided capability declarations
     */
    public List<Capability> getProvidedCapabilities() {
        return capabilities.getProvidedCapabilities();
    }

    /**
     * Returns capabilities required by this extension.
     *
     * @return required capability declarations
     */
    public List<Capability> getRequiredCapabilities() {
        return capabilities.getRequiredCapabilities();
    }

    /**
     * Configures provided and required capabilities.
     *
     * @param capabilitiesAction the capability configuration action
     */
    public void capabilities(Action<Capabilities> capabilitiesAction) {
        capabilitiesAction.execute(this.capabilities);
    }

    /**
     * Returns dependency resource-removal declarations.
     *
     * @return resource-removal declarations
     */
    public List<RemovedResource> getRemoveResources() {
        return removedResources.getRemovedResources();
    }

    /**
     * Configures resources removed from dependency artifacts.
     *
     * @param removedResourcesAction the resource-removal configuration action
     */
    public void removedResources(Action<RemovedResources> removedResourcesAction) {
        removedResourcesAction.execute(this.removedResources);
    }
}
