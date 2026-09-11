package io.quarkus.cyclonedx.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * CycloneDX SBOM generator configuration
 */
@ConfigMapping(prefix = "quarkus.cyclonedx")
@ConfigRoot
public interface CycloneDxConfig {
    /**
     * Whether to skip SBOM generation
     */
    @WithDefault("false")
    boolean skip();

    /**
     * SBOM file format. Supported formats are {code json} and {code xml}.
     * The default format is JSON.
     * If both are desired then {@code all} could be used as the value of this option.
     *
     * @return SBOM file format
     */
    @WithDefault("json")
    String format();

    /**
     * CycloneDX specification version. The default value be the latest supported by the integrated CycloneDX library.
     *
     * @return CycloneDX specification version
     */
    Optional<String> schemaVersion();

    /**
     * Whether to include the license text into generated SBOMs.
     *
     * @return whether to include the license text into generated SBOMs
     */
    @WithDefault("false")
    boolean includeLicenseText();

    /**
     * When Quarkus platform members include product information (a CPE and the extensions/artifacts bound to
     * an offering) and this option is enabled, each product is represented in the SBOM as a top-level component
     * of type {@code framework} that {@code provides} the artifacts attributed to it (CycloneDX 1.6
     * {@code dependency.provides}; recorded as {@code dependsOn} on older schema versions).
     *
     * @return whether to perform platform-member product attribution
     */
    @WithDefault("true")
    boolean productAttribution();

    /**
     * Embedded dependency SBOM configuration
     */
    @ConfigDocSection
    EmbeddedSbomConfig embedded();

    /**
     * Embedded dependency SBOM configuration
     */
    interface EmbeddedSbomConfig {

        /**
         * Whether a dependency SBOM should be embedded in the final application as a classpath resource.
         *
         * @return true, if a dependency SBOM should be embedded in the final application, false - otherwise
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Classpath resource name for the embedded dependency SBOM. The SBOM format (JSON or XML) is
         * derived from the resource name extension.
         *
         * @return resource name for the embedded dependency SBOM
         */
        @WithDefault("META-INF/sbom/dependency.cdx.json")
        String resourceName();
    }
}
