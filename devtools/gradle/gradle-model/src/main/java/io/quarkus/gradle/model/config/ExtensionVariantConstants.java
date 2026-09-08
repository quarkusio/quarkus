package io.quarkus.gradle.model.config;

import org.gradle.api.attributes.Attribute;

public final class ExtensionVariantConstants {

    public static final String EXTENSION_DEPLOYMENT_PLUGIN_ID = "io.quarkus.extension.deployment";
    public static final String EXTENSION_DEPLOYMENT_MARKER_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionDeploymentMarkerElements";
    public static final String EXTENSION_DEPLOYMENT_MARKER_TASK_NAME = "quarkusExtensionDeploymentMarker";
    public static final String EXTENSION_DEPLOYMENT_MARKER_CATEGORY = "quarkus-extension-deployment-marker";
    public static final String EXTENSION_DEPLOYMENT_DEPENDENCY_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionDeploymentDependencyElements";
    public static final String EXTENSION_DEPLOYMENT_DEPENDENCY_CATEGORY = "quarkus-extension-deployment-dependency";
    public static final Attribute<Boolean> EXTENSION_RUNTIME_ATTRIBUTE = Attribute.of("io.quarkus.extension.runtime",
            Boolean.class);
    public static final Attribute<Boolean> EXTENSION_DEPLOYMENT_ATTRIBUTE = Attribute.of(EXTENSION_DEPLOYMENT_PLUGIN_ID,
            Boolean.class);
    public static final Attribute<Boolean> EXTENSION_DEPLOYMENT_DEPENDENCY_ATTRIBUTE = Attribute.of(
            EXTENSION_DEPLOYMENT_PLUGIN_ID + ".dependency", Boolean.class);
    public static final String QUARKUS_ANNOTATION_PROCESSOR = "io.quarkus:quarkus-extension-processor";

    private ExtensionVariantConstants() {
    }
}
