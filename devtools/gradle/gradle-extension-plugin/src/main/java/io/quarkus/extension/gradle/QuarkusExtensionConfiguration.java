package io.quarkus.extension.gradle;

import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import io.quarkus.extension.gradle.dsl.Capabilities;
import io.quarkus.extension.gradle.dsl.Capability;
import io.quarkus.extension.gradle.dsl.RemovedResource;
import io.quarkus.extension.gradle.dsl.RemovedResources;

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

    private Project project;

    public QuarkusExtensionConfiguration(Project project) {
        this.project = project;
        disableValidation = project.getObjects().property(Boolean.class);
        disableValidation.convention(false);
        deploymentArtifact = project.getObjects().property(String.class);
        deploymentModule = project.getObjects().property(String.class);
        deploymentModule.convention("deployment");

        excludedArtifacts = project.getObjects().listProperty(String.class);
        parentFirstArtifacts = project.getObjects().listProperty(String.class);
        runnerParentFirstArtifacts = project.getObjects().listProperty(String.class);
        lesserPriorityArtifacts = project.getObjects().listProperty(String.class);
        conditionalDependencies = project.getObjects().listProperty(String.class);
        conditionalDevDependencies = project.getObjects().listProperty(String.class);
        dependencyCondition = project.getObjects().listProperty(String.class);
    }

    public void setDisableValidation(boolean disableValidation) {
        this.disableValidation.set(disableValidation);
    }

    public Property<Boolean> isValidationDisabled() {
        return disableValidation;
    }

    public Property<String> getDeploymentArtifact() {
        return deploymentArtifact;
    }

    public void setDeploymentArtifact(String deploymentArtifact) {
        this.deploymentArtifact.set(deploymentArtifact);
    }

    public Property<String> getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(String deploymentModule) {
        this.deploymentModule.set(deploymentModule);
    }

    public ListProperty<String> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    public void setExcludedArtifacts(List<String> excludedArtifacts) {
        this.excludedArtifacts.addAll(excludedArtifacts);
    }

    public ListProperty<String> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    public void setParentFirstArtifacts(List<String> parentFirstArtifacts) {
        this.parentFirstArtifacts.addAll(parentFirstArtifacts);
    }

    public ListProperty<String> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    public void setRunnerParentFirstArtifacts(List<String> runnerParentFirstArtifacts) {
        this.runnerParentFirstArtifacts.addAll(runnerParentFirstArtifacts);
    }

    public ListProperty<String> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    public void setLesserPriorityArtifacts(List<String> lesserPriorityArtifacts) {
        this.lesserPriorityArtifacts.addAll(lesserPriorityArtifacts);
    }

    public ListProperty<String> getConditionalDependencies() {
        return conditionalDependencies;
    }

    public void setConditionalDependencies(List<String> conditionalDependencies) {
        this.conditionalDependencies.addAll(conditionalDependencies);
    }

    public ListProperty<String> getConditionalDevDependencies() {
        return conditionalDevDependencies;
    }

    public void setConditionalDevDependencies(List<String> conditionalDependencies) {
        this.conditionalDevDependencies.addAll(conditionalDependencies);
    }

    public ListProperty<String> getDependencyConditions() {
        return dependencyCondition;
    }

    public void setDependencyConditions(List<String> dependencyCondition) {
        this.dependencyCondition.addAll(dependencyCondition);
    }

    public List<Capability> getProvidedCapabilities() {
        return capabilities.getProvidedCapabilities();
    }

    public List<Capability> getRequiredCapabilities() {
        return capabilities.getRequiredCapabilities();
    }

    public void capabilities(Action<Capabilities> capabilitiesAction) {
        capabilitiesAction.execute(this.capabilities);
    }

    public List<RemovedResource> getRemoveResources() {
        return removedResources.getRemovedResources();
    }

    public void removedResources(Action<RemovedResources> removedResourcesAction) {
        removedResourcesAction.execute(this.removedResources);
    }

    public String getDefaultDeployementArtifactName() {
        String projectName = project.getName();
        if (project.getParent() != null && projectName.equals("runtime")) {
            projectName = project.getParent().getName();
        }
        return String.format("%s:%s-deployment:%s", project.getGroup(), projectName,
                project.getVersion());
    }

}
