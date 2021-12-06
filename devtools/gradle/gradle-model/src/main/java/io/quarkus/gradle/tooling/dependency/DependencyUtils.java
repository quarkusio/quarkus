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
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactKey;

public class DependencyUtils {

    private static final String COPY_CONFIGURATION_NAME = "quarkusDependency";
    private static final String TEST_FIXTURE_SUFFIX = "-test-fixtures";

    public static Configuration duplicateConfiguration(Project project, Configuration toDuplicate) {
        Configuration configurationCopy = project.getConfigurations().findByName(COPY_CONFIGURATION_NAME);
        if (configurationCopy != null) {
            project.getConfigurations().remove(configurationCopy);
        }
        configurationCopy = project.getConfigurations().create(COPY_CONFIGURATION_NAME);

        // We add boms for dependency resolution
        List<Dependency> boms = ToolingUtils.getEnforcedPlatforms(toDuplicate);
        configurationCopy.getDependencies().addAll(boms);

        configurationCopy.getDependencyConstraints().addAll(toDuplicate.getAllDependencyConstraints());
        for (Dependency dependency : toDuplicate.getAllDependencies()) {
            if (isTestFixtureDependency(dependency)) {
                continue;
            }
            configurationCopy.getDependencies().add(dependency);
        }
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

    public static String asDependencyNotation(AppArtifactCoords artifactCoords) {
        return String.join(":", artifactCoords.getGroupId(), artifactCoords.getArtifactId(), artifactCoords.getVersion());
    }

    public static ExtensionDependency getExtensionInfoOrNull(Project project, ResolvedArtifact artifact) {
        ModuleVersionIdentifier artifactId = artifact.getModuleVersion().getId();
        File artifactFile = artifact.getFile();

        if (artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
            final Project projectDep = project.getRootProject().findProject(
                    ((ProjectComponentIdentifier) artifact.getId().getComponentIdentifier()).getProjectPath());
            final JavaPluginConvention javaExtension = projectDep == null ? null
                    : projectDep.getConvention().findPlugin(JavaPluginConvention.class);
            if (javaExtension != null) {
                SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
                Path descriptorPath = resourcesDir.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return loadExtensionInfo(project, descriptorPath, artifactId, projectDep);
                }
            }
        }

        if (!artifactFile.exists()) {
            return null;
        }
        if (artifactFile.isDirectory()) {
            Path descriptorPath = artifactFile.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.exists(descriptorPath)) {
                return loadExtensionInfo(project, descriptorPath, artifactId, null);
            }
        } else if ("jar".equals(artifact.getExtension())) {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactFile.toPath())) {
                Path descriptorPath = artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return loadExtensionInfo(project, descriptorPath, artifactId, null);
                }
            } catch (IOException e) {
                throw new GradleException("Failed to read " + artifactFile, e);
            }
        }
        return null;
    }

    private static ExtensionDependency loadExtensionInfo(Project project, Path descriptorPath,
            ModuleVersionIdentifier exentionId, Project extensionProject) {
        final Properties extensionProperties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(descriptorPath)) {
            extensionProperties.load(reader);
        } catch (IOException e) {
            throw new GradleException("Failed to load " + descriptorPath, e);
        }
        AppArtifactCoords deploymentModule = AppArtifactCoords
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
        if (extensionProject != null) {
            return new LocalExtensionDependency(extensionProject, exentionId, deploymentModule, conditionalDependencies,
                    constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
        }
        return new ExtensionDependency(exentionId, deploymentModule, conditionalDependencies,
                constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
    }

    public static Dependency create(DependencyHandler dependencies, String conditionalDependency) {
        AppArtifactCoords dependencyCoords = AppArtifactCoords.fromString(conditionalDependency);
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
        ExternalDependency dependency = (ExternalDependency) dependencies.add(deploymentConfigurationName,
                extension.asDependencyNotation());
        dependency.capabilities(
                handler -> handler.requireCapability(asCapabilityNotation(extension.getDeploymentModule())));
    }

    public static String asCapabilityNotation(AppArtifactCoords artifactCoords) {
        return String.join(":", artifactCoords.getGroupId(), artifactCoords.getArtifactId() + "-capability",
                artifactCoords.getVersion());
    }
}
