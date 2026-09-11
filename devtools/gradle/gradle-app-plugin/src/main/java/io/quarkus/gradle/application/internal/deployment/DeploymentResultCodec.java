package io.quarkus.gradle.application.internal.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import io.quarkus.gradle.application.internal.ResultReceiptProperties;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

public final class DeploymentResultCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String BUILD_NAME = "build.name";
    private static final String DEPLOYMENT_NAME = "deployment.name";
    private static final String DEPLOYMENT_TARGET = "deployment.target";
    private static final String IMAGE_SOURCE = "image.source";
    private static final String IMAGE_REFERENCE = "image.reference";
    private static final String QUARKUS_DEPLOY_TARGET = "quarkus.deploy.target";
    private static final String KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    private static final String RESULT_NAME = "result.name";
    private static final String RESULT_LABEL_PREFIX = "result.labels.";
    private static final String SUCCESS = "success";

    public void write(Path file, DeploymentResult result) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ResultReceiptProperties.store(toProperties(result), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write deployment result receipt " + file, e);
        }
    }

    public DeploymentResult read(Path file) {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read deployment result receipt " + file, e);
        }
        String schemaVersion = required(file, properties, SCHEMA_VERSION);
        if (!DeploymentResult.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported schema version in " + file + ": " + schemaVersion);
        }
        return new DeploymentResult(
                required(file, properties, BUILD_NAME),
                required(file, properties, DEPLOYMENT_NAME),
                target(file, required(file, properties, DEPLOYMENT_TARGET)),
                imageSource(file, required(file, properties, IMAGE_SOURCE)),
                required(file, properties, IMAGE_REFERENCE),
                optional(properties, QUARKUS_DEPLOY_TARGET),
                optional(properties, KUBERNETES_DEPLOYMENT_TARGET),
                optional(properties, RESULT_NAME),
                labels(properties),
                bool(file, required(file, properties, SUCCESS), SUCCESS));
    }

    private static Map<String, String> toProperties(DeploymentResult result) {
        Map<String, String> properties = new TreeMap<>();
        properties.put(SCHEMA_VERSION, DeploymentResult.SCHEMA_VERSION);
        properties.put(BUILD_NAME, result.buildName());
        properties.put(DEPLOYMENT_NAME, result.deploymentName());
        properties.put(DEPLOYMENT_TARGET, result.target().quarkusDeployTarget());
        properties.put(IMAGE_SOURCE, result.imageSource().name());
        properties.put(IMAGE_REFERENCE, result.imageReference());
        result.quarkusDeployTarget().ifPresent(value -> properties.put(QUARKUS_DEPLOY_TARGET, value));
        result.kubernetesDeploymentTarget().ifPresent(value -> properties.put(KUBERNETES_DEPLOYMENT_TARGET, value));
        result.resultName().ifPresent(value -> properties.put(RESULT_NAME, value));
        result.resultLabels().forEach((key, value) -> properties.put(RESULT_LABEL_PREFIX + key, value));
        properties.put(SUCCESS, Boolean.toString(result.success()));
        return properties;
    }

    private static String required(Path file, Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + key + "' in " + file);
        }
        return value;
    }

    private static Optional<String> optional(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key)).filter(value -> !value.isBlank());
    }

    private static boolean bool(Path file, String value, String key) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean field '" + key + "' in " + file + ": " + value);
    }

    private static QuarkusApplicationDeploymentTarget target(Path file, String value) {
        for (QuarkusApplicationDeploymentTarget target : QuarkusApplicationDeploymentTarget.values()) {
            if (target.quarkusDeployTarget().equals(value)) {
                return target;
            }
        }
        throw new IllegalArgumentException("Unknown deployment target in " + file + ": " + value);
    }

    private static QuarkusApplicationDeploymentImageSource imageSource(Path file, String value) {
        try {
            return QuarkusApplicationDeploymentImageSource.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown deployment image source in " + file + ": " + value, e);
        }
    }

    private static Map<String, String> labels(Properties properties) {
        Map<String, String> labels = new LinkedHashMap<>();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(RESULT_LABEL_PREFIX))
                .sorted()
                .forEach(key -> labels.put(key.substring(RESULT_LABEL_PREFIX.length()), properties.getProperty(key)));
        return labels;
    }
}
