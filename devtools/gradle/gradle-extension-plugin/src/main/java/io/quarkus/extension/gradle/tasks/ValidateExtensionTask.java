package io.quarkus.extension.gradle.tasks;

import static io.quarkus.extension.gradle.tasks.Util.artifactType;
import static io.quarkus.extension.gradle.tasks.Util.classifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Validates dependency separation between a Quarkus extension's runtime and deployment modules.
 * <p>
 * The {@code io.quarkus.extension} plugin registers this task as
 * {@link io.quarkus.extension.gradle.QuarkusExtensionPlugin#VALIDATE_EXTENSION_TASK_NAME}. It rejects extension
 * deployment artifacts on the runtime classpath and, for a local deployment project, verifies that all deployment
 * artifacts required by runtime extension dependencies are present. Selecting the deployment marker also verifies that
 * the local deployment project applies {@code io.quarkus.extension.deployment}.
 * <p>
 * The task is skipped when {@code quarkusExtension.disableValidation} is {@code true}. Local deployment checks are
 * skipped when an explicit published deployment artifact is configured. This non-cacheable, plugin-configured type is
 * not a general task-registration or subclassing API.
 */
@DisableCachingByDefault(because = "Not cacheable")
public abstract class ValidateExtensionTask extends DefaultTask {

    /**
     * Creates and wires the plugin-owned validation task.
     *
     * @param quarkusExtensionConfiguration the runtime extension DSL configuration
     * @param runtimeModuleClasspath the runtime module classpath
     * @param deploymentModuleClasspath the local deployment module classpath
     * @param deploymentMarker the local deployment marker configuration
     * @param localDeploymentValidationEnabled whether local deployment validation applies
     */
    @Inject
    public ValidateExtensionTask(QuarkusExtensionConfiguration quarkusExtensionConfiguration,
            Configuration runtimeModuleClasspath, Configuration deploymentModuleClasspath,
            Configuration deploymentMarker, Provider<Boolean> localDeploymentValidationEnabled) {
        setDescription("Validate extension dependencies");
        setGroup("quarkus");

        var runtimeClasspathArtifacts = runtimeModuleClasspath.getIncoming().getArtifacts();

        setDeploymentModuleClasspath(deploymentModuleClasspath, localDeploymentValidationEnabled);
        setDeploymentMarker(deploymentMarker, localDeploymentValidationEnabled);

        getRuntimeModuleArtifacts().set(artifactIds(runtimeClasspathArtifacts));
        getRuntimeExtensionDeploymentArtifacts().set(runtimeExtensionDeploymentArtifacts(runtimeClasspathArtifacts));
        getDeploymentModuleArtifacts().convention(Collections.emptyList());
        getValidationDisabled().set(quarkusExtensionConfiguration.isValidationDisabled());
        getLocalDeploymentValidationEnabled().convention(true);

        this.onlyIf(t -> !getValidationDisabled().get());
    }

    /**
     * Returns Gradle's provider factory used for lazy marker selection.
     *
     * @return the provider factory
     */
    @Inject
    public abstract ProviderFactory getProviders();

    /**
     * Returns Gradle's object factory used for lazy empty file collections.
     *
     * @return the object factory
     */
    @Inject
    public abstract ObjectFactory getObjects();

    /**
     * Returns normalized runtime-module artifact identities.
     *
     * @return runtime artifact identities
     */
    @Input
    public abstract ListProperty<String> getRuntimeModuleArtifacts();

    /**
     * Returns normalized artifacts present in the local deployment module.
     *
     * @return deployment artifact identities
     */
    @Input
    public abstract ListProperty<String> getDeploymentModuleArtifacts();

    /**
     * Returns deployment artifact keys declared by extensions on the runtime classpath.
     *
     * @return runtime extension deployment artifact keys
     */
    @Input
    public abstract ListProperty<String> getRuntimeExtensionDeploymentArtifacts();

    /**
     * Returns whether validation is disabled through the runtime extension DSL.
     *
     * @return whether validation is disabled
     */
    @Input
    public abstract Property<Boolean> getValidationDisabled();

    /**
     * Returns whether the configured deployment artifact is represented by a local deployment project.
     *
     * @return whether local deployment checks are enabled
     */
    @Input
    public abstract Property<Boolean> getLocalDeploymentValidationEnabled();

    /**
     * Returns the marker selected from the local deployment project when local validation is enabled.
     *
     * @return the deployment marker files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getDeploymentMarker();

    private void setDeploymentModuleClasspath(Configuration deploymentModuleClasspath,
            Provider<Boolean> localDeploymentValidationEnabled) {
        getLocalDeploymentValidationEnabled().set(localDeploymentValidationEnabled);
        var resolutionResult = deploymentModuleClasspath.getIncoming().getResolutionResult();
        getDeploymentModuleArtifacts().set(getProject().getProviders().provider(() -> {
            if (!shouldValidateLocalDeployment()) {
                return Collections.emptyList();
            }
            return componentIds(resolutionResult);
        }));
    }

    private void setDeploymentMarker(Configuration deploymentMarker, Provider<Boolean> localDeploymentValidationEnabled) {
        getLocalDeploymentValidationEnabled().set(localDeploymentValidationEnabled);
        var deploymentFiles = deploymentMarker.getIncoming().getFiles();
        getDeploymentMarker().from(getProviders().provider(() -> {
            if (!shouldValidateLocalDeployment()) {
                return getObjects().fileCollection();
            }
            return deploymentFiles;
        }));
    }

    private boolean shouldValidateLocalDeployment() {
        return !getValidationDisabled().get() && getLocalDeploymentValidationEnabled().get();
    }

    private static Provider<List<String>> artifactIds(ArtifactCollection artifacts) {
        return artifacts.getResolvedArtifacts().map(resolvedArtifactResults -> resolvedArtifactResults.stream()
                .filter(artifact -> artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier)
                .map(artifact -> {
                    var id = (ModuleComponentIdentifier) artifact.getId().getComponentIdentifier();
                    return id.getGroup() + ':' + id.getModule() + ':' + id.getVersion() + ':'
                            + classifier(id.getModule(), id.getVersion(), artifact.getFile()) + ':'
                            + artifactType(artifact);
                })
                .sorted()
                .toList());
    }

    private static List<String> componentIds(ResolutionResult resolutionResult) {
        return resolutionResult.getAllComponents().stream()
                .map(ResolvedComponentResult::getModuleVersion)
                .filter(Objects::nonNull)
                .map(moduleVersion -> moduleVersion.getGroup() + ':'
                        + moduleVersion.getName() + ':'
                        + moduleVersion.getVersion() + "::jar")
                .sorted()
                .collect(Collectors.toList());
    }

    private static Provider<List<String>> runtimeExtensionDeploymentArtifacts(ArtifactCollection artifacts) {
        return artifacts.getResolvedArtifacts().map(resolvedArtifactResults -> resolvedArtifactResults.stream()
                .map(artifact -> {
                    ArtifactKey deploymentKey = deploymentArtifactKeyOrNull(artifact);
                    if (deploymentKey != null) {
                        return deploymentKey.getGroupId() + ':' + deploymentKey.getArtifactId();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted()
                .toList());
    }

    private static ArtifactKey deploymentArtifactKeyOrNull(ResolvedArtifactResult artifact) {
        try {
            var artifactFile = artifact.getFile();
            if (!artifactFile.exists()) {
                return null;
            }

            if (artifactFile.isDirectory()) {
                Path descriptorPath = artifactFile.toPath().resolve(BootstrapConstants.DESCRIPTOR_PATH);
                if (Files.isRegularFile(descriptorPath)) {
                    return readDeploymentArtifactKey(descriptorPath);
                }
            } else if (ArtifactCoords.TYPE_JAR.equals(artifactType(artifact))) {
                try (FileSystem artifactFileSystem = ZipUtils.newFileSystem(artifactFile.toPath())) {
                    Path descriptorPath = artifactFileSystem.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                    if (Files.exists(descriptorPath)) {
                        return readDeploymentArtifactKey(descriptorPath);
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new GradleException("Failed to read " + artifact.getFile(), e);
        }
    }

    private static ArtifactKey readDeploymentArtifactKey(Path descriptorPath) throws IOException {
        Properties descriptor = new Properties();
        try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
            descriptor.load(inputStream);
        }
        String deploymentArtifact = descriptor.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if (deploymentArtifact == null) {
            return null;
        }
        ArtifactCoords deploymentCoords = ArtifactCoords.fromString(deploymentArtifact);
        return ArtifactKey.ga(deploymentCoords.getGroupId(), deploymentCoords.getArtifactId());
    }

    /**
     * Performs runtime/deployment separation and completeness checks.
     *
     * @throws GradleException when dependency separation or local deployment completeness is invalid
     */
    @TaskAction
    public void validateExtension() {
        if (shouldValidateLocalDeployment()) {
            getDeploymentMarker().getFiles();
        }
        List<ArtifactKey> deploymentModuleKeys = artifactKeys(getRuntimeExtensionDeploymentArtifacts().get());
        List<ArtifactKey> invalidRuntimeArtifacts = findExtensionInConfiguration(getRuntimeModuleArtifacts().get(),
                deploymentModuleKeys);

        if (shouldValidateLocalDeployment()) {
            List<ArtifactKey> existingDeploymentModuleKeys = findExtensionInConfiguration(getDeploymentModuleArtifacts().get(),
                    deploymentModuleKeys);
            deploymentModuleKeys.removeAll(existingDeploymentModuleKeys);
        } else {
            deploymentModuleKeys.clear();
        }

        boolean hasErrors = !invalidRuntimeArtifacts.isEmpty() || !deploymentModuleKeys.isEmpty();

        if (hasErrors) {
            printValidationErrors(invalidRuntimeArtifacts, deploymentModuleKeys);
        }
    }

    private static List<ArtifactKey> artifactKeys(List<String> artifactIds) {
        return artifactIds.stream()
                .map(ValidateExtensionTask::toArtifactKey)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<ArtifactKey> findExtensionInConfiguration(List<String> deploymentArtifacts,
            List<ArtifactKey> extensions) {
        List<ArtifactKey> foundExtensions = new ArrayList<>();

        for (String deploymentArtifact : deploymentArtifacts) {
            ArtifactKey key = toArtifactKey(deploymentArtifact);
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
                log.error("- {}", invalidRuntimeArtifact);
            }
        }

        if (!missingDeploymentArtifacts.isEmpty()) {
            log.error("The following deployment artifact(s) were found to be missing in the deployment module: ");
            for (ArtifactKey missingDeploymentArtifact : missingDeploymentArtifacts) {
                log.error("- {}", missingDeploymentArtifact);
            }
        }

        throw new GradleException("Quarkus Extension Dependency Verification Error. See logs below");
    }

    private static ArtifactKey toArtifactKey(String artifactId) {
        int firstSeparator = artifactId.indexOf(':');
        int secondSeparator = artifactId.indexOf(':', firstSeparator + 1);
        if (secondSeparator < 0) {
            return ArtifactKey.ga(artifactId.substring(0, firstSeparator), artifactId.substring(firstSeparator + 1));
        }
        return ArtifactKey.ga(artifactId.substring(0, firstSeparator),
                artifactId.substring(firstSeparator + 1, secondSeparator));
    }
}
