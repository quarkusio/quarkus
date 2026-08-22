package io.quarkus.gradle.application.tasks;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.deployment.DeploymentConfigValidator;
import io.quarkus.gradle.application.internal.deployment.DeploymentImageSourceRequest;
import io.quarkus.gradle.application.internal.deployment.DeploymentImageSourceResolution;
import io.quarkus.gradle.application.internal.deployment.DeploymentImageSourceResolver;
import io.quarkus.gradle.application.internal.deployment.DeploymentResult;
import io.quarkus.gradle.application.internal.deployment.DeploymentResultCodec;
import io.quarkus.gradle.application.internal.execution.DeploymentRequest;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * Deploys one named Quarkus application deployment.
 * <p>
 * The plugin registers one task for each deployment declared by a named build. The task resolves the configured image
 * source, validates the deployment configuration, performs the external deployment operation, and writes a receipt for
 * downstream consumers. It is deliberately never up-to-date and is not build-cacheable because deployment mutates
 * external cluster state.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Deployments mutate external cluster state")
public abstract class QuarkusApplicationDeployTask extends QuarkusApplicationBuildTask {

    private final DeploymentImageSourceResolver imageSourceResolver = new DeploymentImageSourceResolver();
    private final DeploymentConfigValidator configValidator = new DeploymentConfigValidator();
    private final DeploymentResultCodec resultCodec = new DeploymentResultCodec();

    /**
     * Creates a deployment task that always executes when selected.
     */
    public QuarkusApplicationDeployTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the deployment name from the named-build DSL.
     *
     * @return the deployment name
     */
    @Input
    public abstract Property<String> getDeploymentName();

    /**
     * Returns the selected deployment target.
     *
     * @return the deployment target
     */
    @Input
    public abstract Property<QuarkusApplicationDeploymentTarget> getDeploymentTarget();

    /**
     * Returns the source from which the deployment obtains its container image.
     *
     * @return the image-source selection
     */
    @Input
    public abstract Property<QuarkusApplicationDeploymentImageSource> getImageSource();

    /**
     * Returns an explicitly configured image reference, when the selected image source permits one.
     *
     * @return the optional image reference
     */
    @Input
    @Optional
    public abstract Property<String> getImageReference();

    /**
     * Returns the normal-image push receipt used when that image is the deployment source.
     *
     * @return the optional normal-image receipt
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getNormalImagePushReceiptFile();

    /**
     * Returns the startup-optimized-image push receipt used when that image is the deployment source.
     *
     * @return the optional startup-optimized-image receipt
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getStartupOptimizedImagePushReceiptFile();

    /**
     * Returns the deployment receipt written after a successful deployment.
     *
     * @return the receipt file
     */
    @OutputFile
    public abstract RegularFileProperty getReceiptFile();

    /**
     * Validates and executes the configured deployment.
     */
    @TaskAction
    public void deployApplication() {
        DeploymentImageSourceResolution image = imageSourceResolver.resolve(
                new DeploymentImageSourceRequest(
                        getImageSource().get(),
                        java.util.Optional.ofNullable(getImageReference().getOrNull()),
                        optionalPath(getNormalImagePushReceiptFile()),
                        optionalPath(getStartupOptimizedImagePushReceiptFile())));
        Map<String, String> operationForcedProperties = deploymentOperationProperties(image);
        validateUnforcedConfig(image);
        DeploymentRequest request = new DeploymentRequest(
                buildRequest(operationForcedProperties),
                getDeploymentName().get(),
                getDeploymentTarget().get(),
                getImageSource().get(),
                image.imageReference(),
                getReceiptFile().get().getAsFile().toPath());
        DeploymentResult result = buildOperations().deploy(request);
        resultCodec.write(request.receiptFile(), result);
    }

    private void validateUnforcedConfig(DeploymentImageSourceResolution image) {
        configValidator.validate(
                getBuildName().get(),
                getDeploymentName().get(),
                getDeploymentTarget().get(),
                getImageSource().get(),
                image.imageReference(),
                effectiveConfig(Map.of()).fullValues());
    }

    private Map<String, String> deploymentOperationProperties(DeploymentImageSourceResolution image) {
        Map<String, String> properties = new LinkedHashMap<>();
        String target = getDeploymentTarget().get().quarkusDeployTarget();
        properties.put("quarkus.deploy.target", target);
        properties.put("quarkus." + target + ".deploy", "true");
        properties.put("quarkus.kubernetes.deployment-target", target);
        properties.putAll(image.forcedProperties());
        return properties;
    }

    private static java.util.Optional<Path> optionalPath(RegularFileProperty file) {
        if (!file.isPresent()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(file.get().getAsFile().toPath());
    }
}
