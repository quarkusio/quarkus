package io.quarkus.cyclonedx.endpoint.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * CycloneDX SBOM endpoint configuration
 */
@ConfigMapping(prefix = "quarkus.cyclonedx.endpoint")
@ConfigRoot
public interface CycloneDxEndpointConfig {

    /**
     * Whether the embedded SBOM should be exposed through a REST endpoint.
     *
     * @return true, if the embedded SBOM should be exposed through a REST endpoint, otherwise - false
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * REST endpoint path that will provide an SBOM
     *
     * @return REST endpoint path that will provide an SBOM
     */
    @WithDefault("/.well-known/sbom")
    String path();

}
