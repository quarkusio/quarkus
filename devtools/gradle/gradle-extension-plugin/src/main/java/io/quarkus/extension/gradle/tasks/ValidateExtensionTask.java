package io.quarkus.extension.gradle.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;
import io.quarkus.gradle.tooling.dependency.ArtifactExtensionDependency;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.gradle.tooling.dependency.ProjectExtensionDependency;
import io.quarkus.maven.dependency.ArtifactKey;

@DisableCachingByDefault(because = "Not cacheable")
public class ValidateExtensionTask extends DefaultTask {

    private Configuration runtimeModuleClasspath;
    private Configuration deploymentModuleClasspath;

    // Captured at configuration time so the task action does not call Task.getProject() at execution time, which is
    // deprecated and removed in Gradle 10. This task is not compatible with the configuration cache, so holding a
    // Project reference is acceptable (it is never serialized).
    private final transient Project project;

    @Inject
    public ValidateExtensionTask(QuarkusExtensionConfiguration quarkusExtensionConfiguration,
            Configuration runtimeModuleClasspath) {
        setDescription("Validate extension dependencies");
        setGroup("quarkus");

        this.project = getProject();
        this.runtimeModuleClasspath = runtimeModuleClasspath;
        this.onlyIf(t -> !quarkusExtensionConfiguration.isValidationDisabled().get());

        // Calling this method tells Gradle that it should not fail the build. Side effect is that the configuration
        // cache will be at least degraded, but the build will not fail.
        notCompatibleWithConfigurationCache("The Quarkus Extension Plugin isn't compatible with the configuration cache");
    }

    @Internal
    public Configuration getRuntimeModuleClasspath() {
        return this.runtimeModuleClasspath;
    }

    @Internal
    public Configuration getDeploymentModuleClasspath() {
        return this.deploymentModuleClasspath;
    }

    @Classpath
    public Set<File> getRuntimeModuleClasspathFiles() {
        return getRuntimeModuleClasspath().getFiles();
    }

    @Classpath
    @Optional
    public Set<File> getDeploymentModuleClasspathFiles() {
        if (getDeploymentModuleClasspath() == null) {
            return Collections.emptySet();
        }
        return getDeploymentModuleClasspath().getFiles();
    }

    @Input
    public List<String> getRuntimeModuleArtifacts() {
        return artifactIds(getRuntimeModuleClasspath().getResolvedConfiguration().getResolvedArtifacts());
    }

    @Input
    public List<String> getDeploymentModuleArtifacts() {
        if (getDeploymentModuleClasspath() == null) {
            return List.of();
        }
        return artifactIds(getDeploymentModuleClasspath().getResolvedConfiguration().getResolvedArtifacts());
    }

    public void setDeploymentModuleClasspath(Configuration deploymentModuleClasspath) {
        this.deploymentModuleClasspath = deploymentModuleClasspath;
    }

    private static List<String> artifactIds(Set<ResolvedArtifact> artifacts) {
        return artifacts.stream()
                .map(artifact -> {
                    ResolvedModuleVersion moduleVersion = artifact.getModuleVersion();
                    return moduleVersion.getId().getGroup() + ':'
                            + moduleVersion.getId().getName() + ':'
                            + moduleVersion.getId().getVersion() + ':'
                            + artifact.getClassifier() + ':'
                            + artifact.getExtension();
                })
                .sorted()
                .collect(Collectors.toList());
    }

    @TaskAction
    public void validateExtension() {
        Set<ResolvedArtifact> runtimeArtifacts = getRuntimeModuleClasspath().getResolvedConfiguration().getResolvedArtifacts();

        List<ArtifactKey> deploymentModuleKeys = collectRuntimeExtensionsDeploymentKeys(runtimeArtifacts);
        List<ArtifactKey> invalidRuntimeArtifacts = findExtensionInConfiguration(runtimeArtifacts, deploymentModuleKeys);

        Set<ResolvedArtifact> deploymentArtifacts = getDeploymentModuleClasspath().getResolvedConfiguration()
                .getResolvedArtifacts();
        List<ArtifactKey> existingDeploymentModuleKeys = findExtensionInConfiguration(deploymentArtifacts,
                deploymentModuleKeys);
        deploymentModuleKeys.removeAll(existingDeploymentModuleKeys);

        boolean hasErrors = false;
        if (!invalidRuntimeArtifacts.isEmpty()) {
            hasErrors = true;
        }
        if (!deploymentModuleKeys.isEmpty()) {
            hasErrors = true;
        }

        if (hasErrors) {
            printValidationErrors(invalidRuntimeArtifacts, deploymentModuleKeys);
        }
    }

    private List<ArtifactKey> collectRuntimeExtensionsDeploymentKeys(Set<ResolvedArtifact> runtimeArtifacts) {
        List<ArtifactKey> runtimeExtensions = new ArrayList<>();
        for (ResolvedArtifact resolvedArtifact : runtimeArtifacts) {
            ExtensionDependency<?> extension = DependencyUtils.getExtensionInfoOrNull(project, resolvedArtifact);
            if (extension != null) {
                if (extension instanceof ProjectExtensionDependency) {
                    final ProjectExtensionDependency ped = (ProjectExtensionDependency) extension;

                    runtimeExtensions
                            .add(ArtifactKey.ga(ped.getDeploymentModule().getGroup().toString(),
                                    ped.getDeploymentModule().getName()));
                } else if (extension instanceof ArtifactExtensionDependency) {
                    final ArtifactExtensionDependency aed = (ArtifactExtensionDependency) extension;

                    runtimeExtensions.add(ArtifactKey.ga(aed.getDeploymentModule().getGroupId(),
                            aed.getDeploymentModule().getArtifactId()));
                }
            }
        }
        return runtimeExtensions;
    }

    private List<ArtifactKey> findExtensionInConfiguration(Set<ResolvedArtifact> deploymentArtifacts,
            List<ArtifactKey> extensions) {
        List<ArtifactKey> foundExtensions = new ArrayList<>();

        for (ResolvedArtifact deploymentArtifact : deploymentArtifacts) {
            ArtifactKey key = toAppArtifactKey(deploymentArtifact.getModuleVersion());
            if (extensions.contains(key)) {
                foundExtensions.add(key);
            }
        }
        return foundExtensions;
    }

    private void printValidationErrors(List<ArtifactKey> invalidRuntimeArtifacts,
            List<ArtifactKey> missingDeploymentArtifacts) {
        Logger log = getLogger();
        log.error("Quarkus Extension Dependency Verification Error");

        if (!invalidRuntimeArtifacts.isEmpty()) {
            log.error("The following deployment artifact(s) appear on the runtime classpath: ");
            for (ArtifactKey invalidRuntimeArtifact : invalidRuntimeArtifacts) {
                log.error("- " + invalidRuntimeArtifact);
            }
        }

        if (!missingDeploymentArtifacts.isEmpty()) {
            log.error("The following deployment artifact(s) were found to be missing in the deployment module: ");
            for (ArtifactKey missingDeploymentArtifact : missingDeploymentArtifacts) {
                log.error("- " + missingDeploymentArtifact);
            }
        }

        throw new GradleException("Quarkus Extension Dependency Verification Error. See logs below");
    }

    private static ArtifactKey toAppArtifactKey(ResolvedModuleVersion artifactId) {
        return ArtifactKey.ga(artifactId.getId().getGroup(), artifactId.getId().getName());
    }
}
