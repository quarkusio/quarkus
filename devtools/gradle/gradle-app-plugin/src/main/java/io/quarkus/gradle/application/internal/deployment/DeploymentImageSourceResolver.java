package io.quarkus.gradle.application.internal.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.GradleException;

import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;

public final class DeploymentImageSourceResolver {

    private final BuiltContainerImageResultCodec imageResultCodec = new BuiltContainerImageResultCodec();

    public DeploymentImageSourceResolution resolve(
            DeploymentImageSourceRequest request) {
        return switch (request.imageSource()) {
            case EXISTING_IMAGE -> existingImage(request);
            case NORMAL_IMAGE_PUSH -> receiptImage(request, request.normalImagePushReceipt()
                    .orElseThrow(() -> new GradleException("Normal image-push deployment requires an image receipt")));
            case STARTUP_OPTIMIZED_IMAGE_PUSH -> receiptImage(request, request.startupOptimizedImagePushReceipt()
                    .orElseThrow(
                            () -> new GradleException("Startup-optimized image-push deployment requires an image receipt")));
        };
    }

    private static DeploymentImageSourceResolution existingImage(
            DeploymentImageSourceRequest request) {
        String image = request.explicitImageReference()
                .orElseThrow(() -> new GradleException("Existing-image deployment requires imageReference"));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("quarkus.container-image.image", image);
        properties.put("quarkus.container-image.build", "false");
        properties.put("quarkus.container-image.push", "false");
        return new DeploymentImageSourceResolution(image, properties);
    }

    private DeploymentImageSourceResolution receiptImage(
            DeploymentImageSourceRequest request, Path receipt) {
        if (!Files.isRegularFile(receipt)) {
            throw new GradleException("Deployment image source " + request.imageSource()
                    + " requires image receipt " + receipt + ", but the file does not exist");
        }
        String image = imageResultCodec.read(receipt).reference()
                .orElseThrow(() -> new GradleException("Image receipt " + receipt + " does not contain an image reference"));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("quarkus.container-image.image", image);
        properties.put("quarkus.container-image.build", "false");
        properties.put("quarkus.container-image.push", "false");
        return new DeploymentImageSourceResolution(image, properties);
    }
}
