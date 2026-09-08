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
     * Whether to include only library components in generated SBOMs, excluding
     * generic file components such as non-Maven JARs and other files.
     *
     * @return whether to include only library components
     */
    @WithDefault("false")
    boolean librariesOnly();

    /**
     * Whether to include only runtime dependencies in generated SBOMs, excluding
     * development/build-time dependencies entirely.
     *
     * @return whether to include only runtime dependencies
     */
    @WithDefault("false")
    boolean runtimeOnly();

    /**
     * Whether to include the {@code quarkus:component:scope} custom property on each component
     * in generated SBOMs. This property indicates whether a component is a {@code runtime} or
     * {@code development} dependency. Since development dependencies are now also marked with the
     * standard CycloneDX {@code scope} set to {@code excluded}, the custom property is redundant
     * for most consumers and is disabled by default.
     *
     * @return whether to include the quarkus:component:scope property
     */
    @WithDefault("false")
    boolean includeQuarkusComponentScope();

    /**
     * When Quarkus platform members include product information (a CPE and the extensions/artifacts bound to
     * an offering) and this option is enabled, each product is represented in the SBOM as a top-level component
     * of type {@code framework} that {@code provides} the artifacts attributed to it (CycloneDX 1.6
     * {@code dependency.provides}; recorded as {@code dependsOn} on older schema versions).
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
         * Whether a dependency SBOM should be embedded in the final application.
         *
         * @return true, if dependency SBOM should be embedded in the final application, false - otherwise
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Resource name for the embedded dependency SBOM.
         * The SBOM is always stored uncompressed under this exact name.
         *
         * @return resource name for the embedded dependency SBOM
         */
        @WithDefault("META-INF/sbom/dependency.cdx.json")
        String resourceName();

        /**
         * Controls whether the embedded SBOM is served GZIP-compressed through the
         * <em>endpoint</em>. This option does not affect how the SBOM is stored in the
         * application: the embedded SBOM resource is always stored uncompressed.
         * <ul>
         * <li>if set to {@code true}, the endpoint always serves the SBOM compressed with
         * {@code Content-Encoding: gzip};</li>
         * <li>if set to {@code false}, the endpoint always serves the SBOM uncompressed;</li>
         * <li>if not set, the endpoint negotiates the encoding based on the request's
         * {@code Accept-Encoding} header, serving the SBOM compressed only when the client
         * accepts {@code gzip}.</li>
         * </ul>
         *
         * @return whether to serve the embedded SBOM GZIP-compressed through the endpoint
         */
        Optional<Boolean> compress();
    }

}
