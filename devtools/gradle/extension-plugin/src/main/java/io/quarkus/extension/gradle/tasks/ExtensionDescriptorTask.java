package io.quarkus.extension.gradle.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;

/**
 * Task that generates extension descriptor files.
 */
public class ExtensionDescriptorTask extends DefaultTask {

    private String deployment;
    private List<String> excludedArtifacts;
    private List<String> parentFirstArtifacts;
    private List<String> runnerParentFirstArtifacts;
    private List<String> lesserPriorityArtifacts;
    private List<String> conditionalDependencies;
    private List<String> dependencyCondition;

    public ExtensionDescriptorTask() {
        setDescription("Generate extension descriptor file");
        setGroup("quarkus");
    }

    @Input
    public String getDeployment() {
        if (deployment == null) {
            String projectName = getProject().getName();
            if (getProject().getParent() != null && projectName.equals("runtime")) {
                projectName = getProject().getParent().getName();
            }
            return String.format("%s:%s-deployment:%s", getProject().getGroup(), projectName,
                    getProject().getVersion());
        }
        return deployment;
    }

    @Option(option = "deployment", description = "GAV of the deployment module")
    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    @Input
    @Optional
    public List<String> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    @Option(option = "excludedArtifacts", description = "Artifacts that should be excluded from the final build")
    public void setExcludedArtifacts(List<String> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
    }

    @Input
    @Optional
    public List<String> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    @Option(option = "parentFirstArtifacts", description = "Artifacts that should be loaded first when running in dev or test mode")
    public void setParentFirstArtifacts(List<String> parentFirstArtifacts) {
        this.parentFirstArtifacts = parentFirstArtifacts;
    }

    @Input
    @Optional
    public List<String> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    @Option(option = "runnerParentFirstArtifacts", description = "Artifacts that should be loaded first when the fast-jar is  used")
    public void setRunnerParentFirstArtifacts(List<String> runnerParentFirstArtifacts) {
        this.runnerParentFirstArtifacts = runnerParentFirstArtifacts;
    }

    @Input
    @Optional
    public List<String> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    @Option(option = "lesserPriorityArtifacts", description = "Artifacts that should be loaded in case no other normal element exists")
    public void setLesserPriorityArtifacts(List<String> lesserPriorityArtifacts) {
        this.lesserPriorityArtifacts = lesserPriorityArtifacts;
    }

    @Input
    @Optional
    public List<String> getConditionalDependencies() {
        return conditionalDependencies;
    }

    @Option(option = "conditionalDependencies", description = "Artifacts that could be conditionally loaded")
    public void setConditionalDependencies(List<String> conditionalDependencies) {
        this.conditionalDependencies = conditionalDependencies;
    }

    @Input
    @Optional
    public List<String> getDependencyCondition() {
        return dependencyCondition;
    }

    @Option(option = "dependencyCondition", description = "Conditions that should enable this extension (in case this extension is loaded as a conditional dependency)")
    public void setDependencyCondition(List<String> dependencyCondition) {
        this.dependencyCondition = dependencyCondition;
    }

    @TaskAction
    public void generateExtensionDescriptor() {
        JavaPluginConvention convention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        File resourcesDir = convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getResourcesDir();

        generateQuarkusExtensionProperties(resourcesDir);
    }

    private void generateQuarkusExtensionProperties(File outputDir) {
        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, getDeployment());

        if (conditionalDependencies != null && !conditionalDependencies.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            }
            props.setProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
        }
        if (dependencyCondition != null && !dependencyCondition.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            while (i < dependencyCondition.size()) {
                buf.append(' ').append(AppArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(AppModel.PARENT_FIRST_ARTIFACTS, val);
        }

        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(AppModel.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(AppModel.EXCLUDED_ARTIFACTS, val);
        }

        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(AppModel.LESSER_PRIORITY_ARTIFACTS, val);
        }

        final Path output = outputDir.toPath().resolve(BootstrapConstants.META_INF);
        try {
            Files.createDirectories(output);
            try (BufferedWriter writer = Files
                    .newBufferedWriter(output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME))) {
                props.store(writer, "Generated by extension-descriptor");
            }
        } catch (IOException e) {
            throw new GradleException(
                    "Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }
    }
}
