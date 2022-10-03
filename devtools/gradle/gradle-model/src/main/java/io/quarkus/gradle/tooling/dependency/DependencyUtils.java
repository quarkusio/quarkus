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
import java.util.Optional;
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

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.fs.util.ZipUtils;
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

    public static Optional<ExtensionDependency> getOptionalExtensionInfo(Project project, ResolvedArtifact artifact) {
        return loadExtensionDependencyFromProject(artifact, project)
                .or(() -> loadExtensionDependencyFromDir(artifact, project))
                .or(() -> loadExtensionDependencyFromJar(artifact, project));
    }

    private static Optional<ExtensionDependency> loadExtensionDependencyFromProject(ResolvedArtifact artifact,
            Project project) {
        Optional<Project> projectDep = Optional.of(artifact.getId().getComponentIdentifier())
                .filter(ProjectComponentIdentifier.class::isInstance)
                .map(ProjectComponentIdentifier.class::cast)
                .map(ProjectComponentIdentifier::getProjectPath)
                .map(projectPath -> project.getRootProject().findProject(projectPath));

        return projectDep
                .map(Project::getExtensions)
                .map(container -> container.findByType(SourceSetContainer.class))
                .map(container -> container.findByName(SourceSet.MAIN_SOURCE_SET_NAME))
                .map(it -> it.getOutput().getResourcesDir())
                .map(File::toPath)
                .flatMap(resourceDir -> loadOptionalExtensionInfo(project, resourceDir, artifact.getModuleVersion().getId(),
                        projectDep.get()));
    }

    private static Optional<ExtensionDependency> loadExtensionDependencyFromDir(ResolvedArtifact artifact, Project project) {
        return Optional.of(artifact.getFile().toPath()).filter(Files::exists)
                .flatMap(path -> loadOptionalExtensionInfo(project, path, artifact.getModuleVersion().getId(), null));
    }

    private static Optional<ExtensionDependency> loadExtensionDependencyFromJar(ResolvedArtifact artifact, Project project) {
        return Optional.of(artifact)
                .filter(it -> ArtifactCoords.TYPE_JAR.equals(it.getExtension()))
                .filter(it -> Files.exists(it.getFile().toPath()))
                .flatMap(it -> {
                    try (FileSystem artifactFs = ZipUtils.newFileSystem(it.getFile().toPath())) {
                        return loadOptionalExtensionInfo(project, artifactFs.getPath(""), artifact.getModuleVersion().getId(),
                                null);
                    } catch (IOException e) {
                        throw new GradleException("Failed to read " + it.getFile(), e);
                    }
                });
    }

    private static Optional<ExtensionDependency> loadOptionalExtensionInfo(Project project, Path resourcePath,
            ModuleVersionIdentifier extensionId, Project extensionProject) {
        return Optional.of(resourcePath)
                .map(path -> path.resolve(BootstrapConstants.DESCRIPTOR_PATH))
                .filter(Files::exists)
                .map(descriptorPath -> loadExtensionInfo(project, descriptorPath, extensionId, extensionProject));
    }

    private static ExtensionDependency loadExtensionInfo(Project project, Path descriptorPath,
            ModuleVersionIdentifier extensionId, Project extensionProject) {
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
        if (extensionProject != null) {
            return new LocalExtensionDependency(extensionProject, extensionId, deploymentModule, conditionalDependencies,
                    constraints == null ? Collections.emptyList() : Arrays.asList(constraints));
        }
        return new ExtensionDependency(extensionId, deploymentModule, conditionalDependencies,
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
