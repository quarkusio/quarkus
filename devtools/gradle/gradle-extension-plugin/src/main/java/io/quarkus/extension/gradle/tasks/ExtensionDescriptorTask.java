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
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;

/**
 * Task that generates extension descriptor files.
 */
public class ExtensionDescriptorTask extends DefaultTask {

    private QuarkusExtensionConfiguration quarkusExtensionConfiguration;
    private File resourcesDir;

    public ExtensionDescriptorTask() {
        setDescription("Generate extension descriptor file");
        setGroup("quarkus");
    }

    @TaskAction
    public void generateExtensionDescriptor() {
        generateQuarkusExtensionProperties(resourcesDir);
    }

    public void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public void setQuarkusExtensionConfiguration(QuarkusExtensionConfiguration quarkusExtensionConfiguration) {
        this.quarkusExtensionConfiguration = quarkusExtensionConfiguration;
    }

    private void generateQuarkusExtensionProperties(File outputDir) {
        final Properties props = new Properties();
        String deploymentArtifact = quarkusExtensionConfiguration.getDeploymentArtifact();
        if (quarkusExtensionConfiguration.getDeploymentArtifact() == null) {
            deploymentArtifact = quarkusExtensionConfiguration.getDefaultDeployementArtifactName();
        }
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deploymentArtifact);

        List<String> conditionalDependencies = quarkusExtensionConfiguration.getConditionalDependencies();
        if (conditionalDependencies != null && !conditionalDependencies.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            }
            props.setProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
        }

        List<String> dependencyConditions = quarkusExtensionConfiguration.getDependencyConditions();
        if (dependencyConditions != null && !dependencyConditions.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactKey.fromString(dependencyConditions.get(i++)).toGacString());
            while (i < dependencyConditions.size()) {
                buf.append(' ').append(AppArtifactKey.fromString(dependencyConditions.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }

        List<String> parentFirstArtifacts = quarkusExtensionConfiguration.getParentFirstArtifacts();
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(AppModel.PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> runnerParentFirstArtifacts = quarkusExtensionConfiguration.getRunnerParentFirstArtifacts();
        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(AppModel.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> excludedArtifacts = quarkusExtensionConfiguration.getExcludedArtifacts();
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(AppModel.EXCLUDED_ARTIFACTS, val);
        }

        List<String> lesserPriorityArtifacts = quarkusExtensionConfiguration.getLesserPriorityArtifacts();
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
