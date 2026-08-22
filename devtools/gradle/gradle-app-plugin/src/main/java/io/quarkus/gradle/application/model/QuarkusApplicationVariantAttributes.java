package io.quarkus.gradle.application.model;

import org.gradle.api.attributes.Attribute;

/**
 * Attribute names and values used by the standalone plugin's outgoing package
 * variants.
 * <p>
 * Consumers should request either the complete relocatable package directory
 * or the producer-local primary launcher JAR, then use the build-name and
 * build-type attributes to disambiguate named outputs.
 */
public final class QuarkusApplicationVariantAttributes {

    /**
     * Category for a complete, relocatable Quarkus application package directory.
     */
    public static final String PACKAGE_CATEGORY = "quarkus-application-package";

    /**
     * Category for the primary launcher JAR at its producer-owned package location.
     * Layout launchers may require sibling files and are not independently portable.
     */
    public static final String LAUNCHER_CATEGORY = "quarkus-application-launcher";

    /**
     * Library-elements value for the complete package-directory contract.
     */
    public static final String PACKAGE_LIBRARY_ELEMENTS = "quarkus-application-package-directory";

    /**
     * Library-elements value for the producer-local launcher-JAR contract.
     */
    public static final String LAUNCHER_LIBRARY_ELEMENTS = "quarkus-application-launcher-jar";

    /**
     * Selects the named build that produced the outgoing artifact.
     */
    public static final Attribute<String> BUILD_NAME_ATTRIBUTE = Attribute.of("io.quarkus.application.build-name",
            String.class);
    /**
     * Selects the package kind produced by the named build.
     */
    public static final Attribute<String> BUILD_TYPE_ATTRIBUTE = Attribute.of("io.quarkus.application.build-type",
            String.class);

    private QuarkusApplicationVariantAttributes() {
    }
}
