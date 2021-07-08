package io.quarkus.gradle.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.util.ZipUtils;

public class ConditionalDependenciesEnabler {

    private final Map<String, ExtensionDependency> featureVariants = new HashMap<>();
    private final Project project;

    public ConditionalDependenciesEnabler(Project project) {
        this.project = project;
    }

    public Set<ExtensionDependency> declareConditionalDependencies(String baseConfigurationName) {
        featureVariants.clear();

        Configuration resolvedConfiguration = DependencyUtils.duplicateConfiguration(project,
                project.getConfigurations().getByName(baseConfigurationName));
        Set<ResolvedArtifact> runtimeArtifacts = resolvedConfiguration.getResolvedConfiguration().getResolvedArtifacts();
        List<ExtensionDependency> extensions = collectExtensionsForResolution(runtimeArtifacts);
        featureVariants.putAll(extractFeatureVariants(extensions));

        resolveConditionalDependencies(extensions, resolvedConfiguration, baseConfigurationName);

        Configuration resolvedExtensionsConfiguration = DependencyUtils.duplicateConfiguration(project,
                project.getConfigurations().getByName(baseConfigurationName));
        Set<ExtensionDependency> enabledExtension = getEnabledExtension(resolvedExtensionsConfiguration);
        project.getConfigurations().remove(resolvedExtensionsConfiguration);

        return enabledExtension;
    }

    private List<ExtensionDependency> collectExtensionsForResolution(Set<ResolvedArtifact> runtimeArtifacts) {
        List<ExtensionDependency> firstLevelExtensions = new ArrayList<>();
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            ExtensionDependency extension = getExtensionInfoOrNull(artifact);
            if (extension != null) {
                if (!extension.conditionalDependencies.isEmpty()) {
                    if (extension.needsResolution(runtimeArtifacts)) {
                        firstLevelExtensions.add(extension);
                    }
                }
            }
        }
        return firstLevelExtensions;
    }

    private Set<ExtensionDependency> getEnabledExtension(Configuration classpath) {
        Set<ExtensionDependency> enabledExtensions = new HashSet<>();
        for (ResolvedArtifact artifact : classpath.getResolvedConfiguration().getResolvedArtifacts()) {
            ExtensionDependency extension = getExtensionInfoOrNull(artifact);
            if (extension != null) {
                enabledExtensions.add(extension);
            }
        }
        return enabledExtensions;
    }

    private Map<String, ExtensionDependency> extractFeatureVariants(List<ExtensionDependency> extensions) {
        Map<String, ExtensionDependency> possibleVariant = new HashMap<>();
        for (ExtensionDependency extension : extensions) {
            for (Dependency dependency : extension.conditionalDependencies) {
                possibleVariant.put(DependencyUtils.asFeatureName(dependency), extension);
            }
        }
        return possibleVariant;
    }

    private void resolveConditionalDependencies(List<ExtensionDependency> conditionalExtensions,
            Configuration existingDependencies, String baseConfigurationName) {
        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(existingDependencies,
                conditionalExtensions);
        boolean hasChanged = false;
        Map<String, ExtensionDependency> validConditionalDependencies = new HashMap<>();
        List<ExtensionDependency> newConditionalDependencies = new ArrayList<>();
        newConditionalDependencies.addAll(conditionalExtensions);
        for (ResolvedArtifact artifact : conditionalDeps.getResolvedConfiguration().getResolvedArtifacts()) {
            ExtensionDependency extensionDependency = getExtensionInfoOrNull(artifact);
            if (extensionDependency != null) {
                if (DependencyUtils.exist(conditionalDeps.getResolvedConfiguration().getResolvedArtifacts(),
                        extensionDependency.dependencyConditions)) {
                    enableConditionalDependency(extensionDependency.extensionId);
                    validConditionalDependencies.put(DependencyUtils.asFeatureName(extensionDependency.extensionId),
                            extensionDependency);
                    if (!extensionDependency.conditionalDependencies.isEmpty()) {
                        featureVariants.putAll(extractFeatureVariants(Collections.singletonList(extensionDependency)));
                    }
                }
                if (!conditionalExtensions.contains(extensionDependency)) {
                    hasChanged = true;
                    newConditionalDependencies.add(extensionDependency);
                }
            }
        }

        Configuration enhancedDependencies = DependencyUtils.duplicateConfiguration(project,
                project.getConfigurations().getByName(baseConfigurationName));

        if (hasChanged) {
            if (!newConditionalDependencies.isEmpty()) {
                resolveConditionalDependencies(newConditionalDependencies, enhancedDependencies, baseConfigurationName);
            }
        }
    }

    private Configuration createConditionalDependenciesConfiguration(Configuration existingDeps,
            List<ExtensionDependency> extensions) {
        List<Dependency> toResolve = new ArrayList<>();
        for (Dependency dependency : existingDeps.getDependencies()) {
            toResolve.add(dependency);
        }
        for (Dependency dependency : collectConditionalDependencies(extensions)) {
            toResolve.add(dependency);
        }
        return project.getConfigurations()
                .detachedConfiguration(toResolve.toArray(new Dependency[0]));
    }

    private Set<Dependency> collectConditionalDependencies(List<ExtensionDependency> extensionDependencies) {
        Set<Dependency> dependencies = new HashSet<>();
        for (ExtensionDependency extensionDependency : extensionDependencies) {
            dependencies.add(extensionDependency.asDependency(project.getDependencies()));
            dependencies.addAll(extensionDependency.conditionalDependencies);
        }
        return dependencies;
    }

    private void enableConditionalDependency(ModuleVersionIdentifier dependency) {
        ExtensionDependency extension = featureVariants.get(DependencyUtils.asFeatureName(dependency));
        if (extension == null) {
            return;
        }
        extension.importConditionalDependency(project.getDependencies(), dependency);
    }

    private ExtensionDependency getExtensionInfoOrNull(ResolvedArtifact artifact) {
        ModuleVersionIdentifier artifactId = artifact.getModuleVersion().getId();
        File artifactFile = artifact.getFile();
        if (!artifactFile.exists()) {
            return null;
        }
        if (artifactFile.isDirectory()) {
            Path descriptorPath = artifactFile.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.exists(descriptorPath)) {
                return loadExtensionInfo(descriptorPath, artifactId);
            }
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactFile.toPath())) {
                Path descriptorPath = artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return loadExtensionInfo(descriptorPath, artifactId);
                }
            } catch (IOException e) {
                throw new GradleException("Failed to read " + artifactFile, e);
            }
        }
        return null;
    }

    private ExtensionDependency loadExtensionInfo(Path descriptorPath, ModuleVersionIdentifier exentionId) {
        final Properties extensionProperties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(descriptorPath)) {
            extensionProperties.load(reader);
        } catch (IOException e) {
            throw new GradleException("Failed to load " + descriptorPath, e);
        }
        AppArtifactCoords deploymentModule = AppArtifactCoords
                .fromString(extensionProperties.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT));
        List<Dependency> conditionalDependencies = new ArrayList<>();
        if (extensionProperties.containsKey(BootstrapConstants.CONDITIONAL_DEPENDENCIES)) {
            String conditionalDeps = extensionProperties.get(BootstrapConstants.CONDITIONAL_DEPENDENCIES).toString();
            for (String conditionalDep : conditionalDeps.split(",")) {
                conditionalDependencies.add(DependencyUtils.create(project.getDependencies(), conditionalDep));
            }
        }
        List<AppArtifactKey> constraints = new ArrayList<>();
        if (extensionProperties.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
            String constraintDeps = extensionProperties.getProperty(BootstrapConstants.DEPENDENCY_CONDITION);
            for (String constraint : constraintDeps.split(",")) {
                constraints.add(AppArtifactKey.fromString(constraint));
            }
        }
        return new ExtensionDependency(exentionId, deploymentModule, conditionalDependencies, constraints);
    }
}
