package io.quarkus.extension.gradle;

import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;

import io.quarkus.extension.gradle.dsl.Capabilities;
import io.quarkus.extension.gradle.dsl.Capability;

public class QuarkusExtensionConfiguration {

    private boolean disableValidation;
    private String deploymentArtifact;
    private String deploymentModule;
    private List<String> excludedArtifacts;
    private List<String> parentFirstArtifacts;
    private List<String> runnerParentFirstArtifacts;
    private List<String> lesserPriorityArtifacts;
    private List<String> conditionalDependencies;
    private List<String> dependencyCondition;
    private Capabilities capabilities = new Capabilities();

    private Project project;

    public QuarkusExtensionConfiguration(Project project) {
        this.project = project;
    }

    public void setDisableValidation(boolean disableValidation) {
        this.disableValidation = disableValidation;
    }

    public boolean isValidationDisabled() {
        return disableValidation;
    }

    public String getDeploymentArtifact() {
        return deploymentArtifact;
    }

    public void setDeploymentArtifact(String deploymentArtifact) {
        this.deploymentArtifact = deploymentArtifact;
    }

    public String getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(String deploymentModule) {
        this.deploymentModule = deploymentModule;
    }

    public List<String> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    public void setExcludedArtifacts(List<String> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
    }

    public List<String> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    public void setParentFirstArtifacts(List<String> parentFirstArtifacts) {
        this.parentFirstArtifacts = parentFirstArtifacts;
    }

    public List<String> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    public void setRunnerParentFirstArtifacts(List<String> runnerParentFirstArtifacts) {
        this.runnerParentFirstArtifacts = runnerParentFirstArtifacts;
    }

    public List<String> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    public void setLesserPriorityArtifacts(List<String> lesserPriorityArtifacts) {
        this.lesserPriorityArtifacts = lesserPriorityArtifacts;
    }

    public List<String> getConditionalDependencies() {
        return conditionalDependencies;
    }

    public void setConditionalDependencies(List<String> conditionalDependencies) {
        this.conditionalDependencies = conditionalDependencies;
    }

    public List<String> getDependencyConditions() {
        return dependencyCondition;
    }

    public void setDependencyConditions(List<String> dependencyCondition) {
        this.dependencyCondition = dependencyCondition;
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

    public String getDefaultDeployementArtifactName() {
        String projectName = project.getName();
        if (project.getParent() != null && projectName.equals("runtime")) {
            projectName = project.getParent().getName();
        }
        return String.format("%s:%s-deployment:%s", project.getGroup(), projectName,
                project.getVersion());
    }

}
