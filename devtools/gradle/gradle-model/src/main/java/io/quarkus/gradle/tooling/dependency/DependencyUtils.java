package io.quarkus.gradle.tooling.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.composite.IncludedBuildInternal;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;

public class DependencyUtils {

    private static final String COPY_CONFIGURATION_NAME = "quarkusDependency";
    private static final String TEST_FIXTURE_SUFFIX = "-test-fixtures";

    public static Configuration duplicateConfiguration(Project project, Configuration toDuplicate) {
        Configuration configurationCopy = project.getConfigurations().findByName(COPY_CONFIGURATION_NAME);
        if (configurationCopy != null) {
            project.getConfigurations().remove(configurationCopy);
        }
        return duplicateConfiguration(project, COPY_CONFIGURATION_NAME, toDuplicate);
    }

    public static Configuration duplicateConfiguration(Project project, String name, Configuration toDuplicate) {
        final Configuration configurationCopy = project.getConfigurations().create(name);
        configurationCopy.getDependencies().addAll(toDuplicate.getAllDependencies());
        configurationCopy.getDependencyConstraints().addAll(toDuplicate.getAllDependencyConstraints());
        return configurationCopy;
    }

    public static boolean isTestFixtureDependency(Dependency dependency) {
        if (!(dependency instanceof ModuleDependency)) {
            return false;
        }
        ModuleDependency module = (ModuleDependency) dependency;
        for (Capability requestedCapability : module.getRequestedCapabilities()) {
            if (requestedCapability.getName().endsWith(TEST_FIXTURE_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    public static String asDependencyNotation(Dependency dependency) {
        return String.join(":", dependency.getGroup(), dependency.getName(), dependency.getVersion());
    }

    public static String asDependencyNotation(ArtifactCoords artifactCoords) {
        return String.join(":", artifactCoords.getGroupId(), artifactCoords.getArtifactId(), artifactCoords.getVersion());
    }

    public static ExtensionDependency getExtensionInfoOrNull(Project project, ResolvedArtifact artifact) {
        ModuleVersionIdentifier artifactId = artifact.getModuleVersion().getId();
        File artifactFile = artifact.getFile();

        if (artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
            ProjectComponentIdentifier componentIdentifier = ((ProjectComponentIdentifier) artifact.getId()
                    .getComponentIdentifier());
            Project projectDep = project.getRootProject().findProject(
                    componentIdentifier.getProjectPath());
            SourceSetContainer sourceSets = projectDep == null ? null
                    : projectDep.getExtensions().findByType(SourceSetContainer.class);
            final String classifier = artifact.getClassifier();
            boolean isIncludedBuild = false;
            if ((!componentIdentifier.getBuild().isCurrentBuild() || sourceSets == null)
                    && (classifier == null || classifier.isEmpty())) {
                var includedBuild = ToolingUtils.includedBuild(project, componentIdentifier);
                if (includedBuild instanceof IncludedBuildInternal) {
                    projectDep = ToolingUtils.includedBuildProject((IncludedBuildInternal) includedBuild, componentIdentifier);
                    sourceSets = projectDep == null ? null : projectDep.getExtensions().findByType(SourceSetContainer.class);
                    isIncludedBuild = true;
                }
            }
            if (sourceSets != null) {
                SourceSet mainSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
                if (mainSourceSet == null) {
                    return null;
                }
                File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
                Path descriptorPath = resourcesDir.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return loadExtensionInfo(project, descriptorPath, artifactId, projectDep, isIncludedBuild);
                }
            }
        }

        if (!artifactFile.exists()) {
            return null;
        }
        if (artifactFile.isDirectory()) {
            Path descriptorPath = artifactFile.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.exists(descriptorPath)) {
                return loadExtensionInfo(project, descriptorPath, artifactId, null, false);
            }
        } else if (ArtifactCoords.TYPE_JAR.equals(artifact.getExtension())) {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactFile.toPath())) {
                Path descriptorPath = artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return loadExtensionInfo(project, descriptorPath, artifactId, null, false);
                }
            } catch (IOException e) {
                throw new GradleException("Failed to read " + artifactFile, e);
            }
        }
        return null;
    }

    private static ExtensionDependency loadExtensionInfo(Project project, Path descriptorPath,
            ModuleVersionIdentifier exentionId, Project extensionProject, boolean isIncludedBuild) {
        final Properties extensionProperties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(descriptorPath)) {
            extensionProperties.load(reader);
        } catch (IOException e) {
            throw new GradleException("Failed to load " + descriptorPath, e);
        }
        ArtifactCoords deploymentModule = GACTV
                .fromString(extensionProperties.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT));
        final List<Dependency> conditionalDependencies;
        if (extensionProperties.containsKey(BootstrapConstants.CONDITIONAL_DEPENDENCIES)) {
            final String[] deps = BootstrapUtils
                    .splitByWhitespace(extensionProperties.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES));
            conditionalDependencies = new ArrayList<>(deps.length);
            for (String conditionalDep : deps) {
                conditionalDependencies.add(create(project.getDependencies(), conditionalDep));
            }
        } else {
            conditionalDependencies = Collections.emptyList();
        }

        final ArtifactKey[] constraints = BootstrapUtils
                .parseDependencyCondition(extensionProperties.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));
        if (isIncludedBuild) {
            return new IncludedBuildExtensionDependency(extensionProject, exentionId, deploymentModule, conditionalDependencies,
                    constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
        }
        if (extensionProject != null) {
            return new LocalExtensionDependency(extensionProject, exentionId, deploymentModule, conditionalDependencies,
                    constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
        }
        return new ExtensionDependency(exentionId, deploymentModule, conditionalDependencies,
                constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
    }

    public static Dependency create(DependencyHandler dependencies, String conditionalDependency) {
        final ArtifactCoords dependencyCoords = GACTV.fromString(conditionalDependency);
        return dependencies.create(String.join(":", dependencyCoords.getGroupId(), dependencyCoords.getArtifactId(),
                dependencyCoords.getVersion()));
    }

    public static void addLocalDeploymentDependency(String deploymentConfigurationName, LocalExtensionDependency extension,
            DependencyHandler dependencies) {
        dependencies.add(deploymentConfigurationName,
                dependencies.project(Collections.singletonMap("path", extension.findDeploymentModulePath())));
    }

    public static void requireDeploymentDependency(String deploymentConfigurationName, ExtensionDependency extension,
            DependencyHandler dependencies) {
        dependencies.add(deploymentConfigurationName,
                extension.getDeploymentModule().getGroupId() + ":" + extension.getDeploymentModule().getArtifactId() + ":"
                        + extension.getDeploymentModule().getVersion());
    }
}
