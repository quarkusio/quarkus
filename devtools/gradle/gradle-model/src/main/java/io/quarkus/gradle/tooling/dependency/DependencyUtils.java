package io.quarkus.gradle.tooling.dependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.composite.IncludedBuildInternal;
import org.jetbrains.annotations.Nullable;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.extension.ConfigurationUtils;
import io.quarkus.gradle.extension.ExtensionConstants;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
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

    public static ExtensionDependency<?> getExtensionInfoOrNull(Project project, ResolvedArtifact artifact) {
        ModuleVersionIdentifier artifactId = artifact.getModuleVersion().getId();

        ExtensionDependency<?> projectDependency;

        if (artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
            ProjectComponentIdentifier componentId = (ProjectComponentIdentifier) artifact.getId().getComponentIdentifier();

            projectDependency = getProjectExtensionDependencyOrNull(
                    project,
                    componentId.getProjectPath(),
                    componentId.getBuild().getName());

            if (projectDependency != null)
                return projectDependency;
        }

        Project localExtensionProject = ToolingUtils.findLocalProject(
                project,
                ArtifactCoords.of(artifactId.getGroup(), artifactId.getName(), null, null, artifactId.getVersion()));

        if (localExtensionProject != null) {
            projectDependency = getExtensionInfoOrNull(project, localExtensionProject);

            if (projectDependency != null)
                return projectDependency;
        }

        File artifactFile = artifact.getFile();
        if (!artifactFile.exists()) {
            return null;
        }

        if (artifactFile.isDirectory()) {
            Path descriptorPath = artifactFile.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.isRegularFile(descriptorPath)) {
                return createExtensionDependency(project, artifactId, descriptorPath);
            }
        } else if (ArtifactCoords.TYPE_JAR.equals(artifact.getExtension())) {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactFile.toPath())) {
                Path descriptorPath = artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.exists(descriptorPath)) {
                    return createExtensionDependency(project, artifactId, descriptorPath);
                }
            } catch (IOException x) {
                throw new GradleException("Failed to read " + artifactFile, x);
            }
        }

        return null;
    }

    public static ExtensionDependency<?> getExtensionInfoOrNull(Project project, Project extensionProject) {
        boolean isIncludedBuild = !project.getRootProject().getGradle().equals(extensionProject.getRootProject().getGradle());

        ModuleVersionIdentifier extensionArtifactId = DefaultModuleVersionIdentifier.newId(
                extensionProject.getGroup().toString(),
                extensionProject.getName(),
                extensionProject.getVersion().toString());

        Object extensionConfiguration = extensionProject
                .getExtensions().findByName(ExtensionConstants.EXTENSION_CONFIGURATION_NAME);

        // If there's an extension configuration file in the project resources it can override
        // certain settings, so we also look for it here.
        Path descriptorPath = findLocalExtensionDescriptorPath(extensionProject);

        if (extensionConfiguration != null || descriptorPath != null) {
            return createExtensionDependency(
                    project,
                    extensionArtifactId,
                    extensionProject,
                    extensionConfiguration,
                    descriptorPath != null ? loadLocalExtensionDescriptor(descriptorPath) : null,
                    isIncludedBuild);
        } else {
            return null;
        }
    }

    private static Path findLocalExtensionDescriptorPath(Project extensionProject) {
        SourceSetContainer sourceSets = extensionProject.getExtensions().getByType(SourceSetContainer.class);
        SourceSet mainSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (mainSourceSet == null) {
            return null;
        }

        Set<File> resourcesSourceDirs = mainSourceSet.getResources().getSrcDirs();
        for (File resourceSourceDir : resourcesSourceDirs) {
            Path descriptorPath = resourceSourceDir.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.isRegularFile(descriptorPath)) {
                return descriptorPath;
            }
        }

        return null;
    }

    private static Properties loadLocalExtensionDescriptor(Path descriptorPath) {
        Properties descriptor = new Properties();
        try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
            descriptor.load(inputStream);
        } catch (IOException x) {
            throw new GradleException("Failed to load extension descriptor at " + descriptorPath, x);
        }

        return descriptor;
    }

    @Nullable
    public static ExtensionDependency<?> getProjectExtensionDependencyOrNull(
            Project project,
            String projectPath,
            @Nullable String buildName) {
        Project extensionProject = project.getRootProject().findProject(projectPath);
        if (extensionProject == null) {
            IncludedBuild extProjIncludedBuild = ToolingUtils.includedBuild(project, buildName);
            if (extProjIncludedBuild instanceof IncludedBuildInternal) {
                extensionProject = ToolingUtils
                        .includedBuildProject((IncludedBuildInternal) extProjIncludedBuild, projectPath);
            }
        }

        if (extensionProject != null) {
            return getExtensionInfoOrNull(project, extensionProject);
        }

        return null;
    }

    private static ProjectExtensionDependency createExtensionDependency(
            Project project,
            ModuleVersionIdentifier extensionArtifactId,
            Project extensionProject,
            @Nullable Object extensionConfiguration,
            @Nullable Properties extensionDescriptor,
            boolean isIncludedBuild) {
        if (extensionConfiguration == null && extensionDescriptor == null) {
            throw new IllegalArgumentException("both extensionConfiguration and extensionDescriptor are null");
        }

        Project deploymentProject = null;

        if (extensionConfiguration != null) {
            final String deploymentProjectPath = ConfigurationUtils.getDeploymentModule(extensionConfiguration).get();
            deploymentProject = ToolingUtils.findLocalProject(extensionProject, deploymentProjectPath);

            if (deploymentProject == null) {
                throw new GradleException("Cannot find deployment project for extension " + extensionArtifactId + " at path "
                        + deploymentProjectPath);
            }
        } else if (extensionDescriptor.containsKey(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT)) {
            final ArtifactCoords deploymentArtifact = GACTV
                    .fromString(extensionDescriptor.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT));

            deploymentProject = ToolingUtils.findLocalProject(project, deploymentArtifact);

            if (deploymentProject == null) {
                throw new GradleException("Cannot find deployment project for extension " + extensionArtifactId
                        + " with artifact coordinates " + deploymentArtifact);
            }
        }

        final List<Dependency> conditionalDependencies = new ArrayList<>();
        final List<ArtifactKey> dependencyConditions = new ArrayList<>();

        if (extensionConfiguration != null) {
            final ListProperty<String> conditionalDependenciesProp = ConfigurationUtils
                    .getConditionalDependencies(extensionConfiguration);

            if (conditionalDependenciesProp.isPresent()) {
                for (String rawDep : conditionalDependenciesProp.get()) {
                    conditionalDependencies.add(create(project.getDependencies(), rawDep));
                }
            }

            final ListProperty<String> dependencyConditionsProp = ConfigurationUtils
                    .getDependencyConditions(extensionConfiguration);

            if (dependencyConditionsProp.isPresent()) {
                for (String rawCond : dependencyConditionsProp.get()) {
                    dependencyConditions.add(GACT.fromString(rawCond));
                }
            }
        }

        if (extensionDescriptor != null && extensionDescriptor.containsKey(BootstrapConstants.CONDITIONAL_DEPENDENCIES)) {
            final String[] deps = BootstrapUtils
                    .splitByWhitespace(extensionDescriptor.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES));

            for (String condDep : deps) {
                conditionalDependencies.add(create(project.getDependencies(), condDep));
            }
        }

        if (extensionDescriptor != null && extensionDescriptor.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
            final ArtifactKey[] conditions = BootstrapUtils
                    .parseDependencyCondition(extensionDescriptor.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));

            dependencyConditions.addAll(Arrays.asList(conditions));
        }

        return new ProjectExtensionDependency(
                extensionProject,
                deploymentProject,
                isIncludedBuild,
                conditionalDependencies,
                dependencyConditions);
    }

    private static ArtifactExtensionDependency createExtensionDependency(
            Project project,
            ModuleVersionIdentifier extensionArtifactId,
            Path descriptorPath) {
        final Properties extensionProperties = loadLocalExtensionDescriptor(descriptorPath);

        final ArtifactCoords deploymentArtifact = GACTV
                .fromString(extensionProperties.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT));

        final List<Dependency> conditionalDependencies;
        if (extensionProperties.containsKey(BootstrapConstants.CONDITIONAL_DEPENDENCIES)) {
            final String[] deps = BootstrapUtils
                    .splitByWhitespace(extensionProperties.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES));

            if (deps.length > 0) {
                conditionalDependencies = new ArrayList<>(deps.length);
                for (String condDep : deps) {
                    conditionalDependencies.add(create(project.getDependencies(), condDep));
                }
            } else {
                conditionalDependencies = Collections.emptyList();
            }
        } else {
            conditionalDependencies = Collections.emptyList();
        }

        final List<ArtifactKey> dependencyConditions;
        if (extensionProperties.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
            final ArtifactKey[] conditions = BootstrapUtils
                    .parseDependencyCondition(extensionProperties.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));

            if (conditions.length > 0) {
                dependencyConditions = Arrays.asList(conditions);
            } else {
                dependencyConditions = Collections.emptyList();
            }
        } else {
            dependencyConditions = Collections.emptyList();
        }

        return new ArtifactExtensionDependency(
                extensionArtifactId,
                deploymentArtifact,
                conditionalDependencies,
                dependencyConditions);
    }

    public static Dependency create(DependencyHandler dependencies, String conditionalDependency) {
        final ArtifactCoords dependencyCoords = GACTV.fromString(conditionalDependency);
        return dependencies.create(String.join(":", dependencyCoords.getGroupId(), dependencyCoords.getArtifactId(),
                dependencyCoords.getVersion()));
    }

    public static Dependency createDeploymentDependency(
            DependencyHandler dependencyHandler,
            ExtensionDependency<?> dependency) {
        if (dependency instanceof ProjectExtensionDependency) {
            ProjectExtensionDependency ped = (ProjectExtensionDependency) dependency;
            return createDeploymentProjectDependency(dependencyHandler, ped);
        } else if (dependency instanceof ArtifactExtensionDependency) {
            ArtifactExtensionDependency aed = (ArtifactExtensionDependency) dependency;
            return createArtifactDeploymentDependency(dependencyHandler, aed);
        }

        throw new IllegalArgumentException("Unknown ExtensionDependency type: " + dependency.getClass().getName());
    }

    private static Dependency createDeploymentProjectDependency(DependencyHandler handler, ProjectExtensionDependency ped) {
        if (ped.isIncludedBuild()) {
            return new DefaultExternalModuleDependency(
                    ped.getDeploymentModule().getGroup().toString(),
                    ped.getDeploymentModule().getName(),
                    ped.getDeploymentModule().getVersion().toString());
        } else if (ped.getDeploymentModule() instanceof ProjectInternal) {
            return handler.create(new DefaultProjectDependency((ProjectInternal) ped.getDeploymentModule(), true));
        } else {
            return handler.create(handler.project(Collections.singletonMap("path", ped.getDeploymentModule().getPath())));
        }
    }

    private static Dependency createArtifactDeploymentDependency(DependencyHandler handler,
            ArtifactExtensionDependency dependency) {
        return handler.create(dependency.getDeploymentModule().getGroupId() + ":"
                + dependency.getDeploymentModule().getArtifactId() + ":"
                + dependency.getDeploymentModule().getVersion());
    }
}
