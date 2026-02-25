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
     * Whether to CycloneDX SBOM generation is enabled.
     * If this option is false, the rest of the configuration will be ignored.
     */
    @WithDefault("true")
    boolean enabled();

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
     * Whether to pretty-print the generated SBOM output.
     *
     * @return whether to pretty-print the generated SBOM output
     */
    @WithDefault("false")
    boolean prettyPrint();

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
         * Whether a dependency SBOM should be embedded in the final application.
         *
         * @return true, if dependency SBOM should be embedded in the final application, false - otherwise
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Base resource name for the embedded dependency SBOM.
         * If {@link #compress()} is enabled, the actual classpath resource name will
         * have a {@code .gz} extension appended (e.g., {@code META-INF/sbom/dependency.cdx.json.gz}).
         *
         * @return base resource name for the embedded dependency SBOM
         */
        @WithDefault("META-INF/sbom/dependency.cdx.json")
        String resourceName();

        /**
         * Whether to compress the embedded SBOM with GZIP.
         * When enabled, the SBOM will be stored compressed in the application
         * with a {@code .gz} extension appended to the {@link #resourceName()},
         * and served compressed through the endpoint with {@code Content-Encoding: gzip}.
         *
         * @return whether to compress the embedded SBOM with GZIP
         */
        @WithDefault("true")
        boolean compress();
    }

}
