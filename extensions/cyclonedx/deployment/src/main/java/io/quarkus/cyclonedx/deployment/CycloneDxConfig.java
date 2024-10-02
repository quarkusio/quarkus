package io.quarkus.cyclonedx.deployment;

import java.util.Optional;

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
}
