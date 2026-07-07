package io.quarkus.gradle.model.config;

import org.gradle.api.attributes.Attribute;

/**
 * Shared names and attributes for the variants exchanged by the Quarkus extension plugins.
 * <p>
 * These constants form a cross-plugin Gradle contract: producers and consumers must use identical configuration,
 * category, capability, and attribute names for variant selection to work. They are plugin implementation API, not
 * application-build DSL.
 */
public final class ExtensionVariantConstants {

    /** Plugin ID used by Quarkus extension deployment projects. */
    public static final String EXTENSION_DEPLOYMENT_PLUGIN_ID = "io.quarkus.extension.deployment";
    /** Consumable configuration that publishes the extension deployment marker. */
    public static final String EXTENSION_DEPLOYMENT_MARKER_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionDeploymentMarkerElements";
    /** Task that creates the extension deployment marker. */
    public static final String EXTENSION_DEPLOYMENT_MARKER_TASK_NAME = "quarkusExtensionDeploymentMarker";
    /** Gradle category used to select an extension deployment marker. */
    public static final String EXTENSION_DEPLOYMENT_MARKER_CATEGORY = "quarkus-extension-deployment-marker";
    /** Consumable configuration that publishes an extension's deployment dependency. */
    public static final String EXTENSION_DEPLOYMENT_DEPENDENCY_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionDeploymentDependencyElements";
    /** Gradle category used to select an extension deployment dependency. */
    public static final String EXTENSION_DEPLOYMENT_DEPENDENCY_CATEGORY = "quarkus-extension-deployment-dependency";
    /** Consumable configuration that publishes an extension's conditional dependencies. */
    public static final String EXTENSION_CONDITIONAL_DEPENDENCIES_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionConditionalDependenciesElements";
    /** Gradle category used to select an extension's conditional dependencies. */
    public static final String EXTENSION_CONDITIONAL_DEPENDENCIES_CATEGORY = "quarkus-extension-conditional-dependencies";
    /** Consumable configuration that publishes an extension's development-only conditional dependencies. */
    public static final String EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_ELEMENTS_CONFIGURATION_NAME = "quarkusExtensionConditionalDevDependenciesElements";
    /** Gradle category used to select development-only conditional dependencies. */
    public static final String EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_CATEGORY = "quarkus-extension-conditional-dev-dependencies";
    /** Boolean attribute identifying the runtime variant of a Quarkus extension. */
    public static final Attribute<Boolean> EXTENSION_RUNTIME_ATTRIBUTE = Attribute.of("io.quarkus.extension.runtime",
            Boolean.class);
    /** Boolean attribute identifying the deployment variant of a Quarkus extension. */
    public static final Attribute<Boolean> EXTENSION_DEPLOYMENT_ATTRIBUTE = Attribute.of(EXTENSION_DEPLOYMENT_PLUGIN_ID,
            Boolean.class);
    /** Boolean attribute identifying the dependency declared by an extension deployment project. */
    public static final Attribute<Boolean> EXTENSION_DEPLOYMENT_DEPENDENCY_ATTRIBUTE = Attribute.of(
            EXTENSION_DEPLOYMENT_PLUGIN_ID + ".dependency", Boolean.class);
    /** Boolean attribute identifying an extension's conditional-dependency metadata. */
    public static final Attribute<Boolean> EXTENSION_CONDITIONAL_DEPENDENCIES_ATTRIBUTE = Attribute.of(
            "io.quarkus.extension.conditional-dependencies", Boolean.class);
    /** Boolean attribute identifying an extension's development-only conditional-dependency metadata. */
    public static final Attribute<Boolean> EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_ATTRIBUTE = Attribute.of(
            "io.quarkus.extension.conditional-dev-dependencies", Boolean.class);
    /** Dependency notation for the Quarkus extension annotation processor. */
    public static final String QUARKUS_ANNOTATION_PROCESSOR = "io.quarkus:quarkus-extension-processor";

    /**
     * Returns the capability notation used for a named extension variant.
     *
     * @param group module group
     * @param name module name
     * @param version module version
     * @param variantName suffix identifying the extension variant
     * @return capability notation in {@code group:name-variantName:version} form
     */
    public static String extensionVariantCapability(String group, String name, String version, String variantName) {
        return group + ":" + name + "-" + variantName + ":" + version;
    }

    private ExtensionVariantConstants() {
    }
}
