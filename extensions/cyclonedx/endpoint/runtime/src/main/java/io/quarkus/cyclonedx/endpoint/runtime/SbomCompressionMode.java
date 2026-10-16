package io.quarkus.cyclonedx.endpoint.runtime;

/**
 * How the SBOM endpoint applies GZIP compression when serving the embedded SBOM.
 */
public enum SbomCompressionMode {

    /**
     * Always serve the SBOM GZIP-compressed with {@code Content-Encoding: gzip}.
     */
    ALWAYS,

    /**
     * Always serve the SBOM uncompressed.
     */
    NEVER,

    /**
     * Serve the SBOM GZIP-compressed only when the client indicates it accepts
     * {@code gzip} through the request's {@code Accept-Encoding} header.
     */
    NEGOTIATE
}
