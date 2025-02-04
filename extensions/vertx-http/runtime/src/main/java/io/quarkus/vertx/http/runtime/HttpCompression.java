package io.quarkus.vertx.http.runtime;

import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;

public enum HttpCompression {
    /**
     * Compression is explicitly enabled.
     *
     * @see Compressed
     */
    ON,
    /**
     * Compression is explicitly disabled.
     *
     * @see Uncompressed
     */
    OFF,
    /**
     * Compression will be enabled if the response has the {@code Content-Type} header set and the value is listed in
     * {@link VertxHttpConfig#compressMediaTypes}.
     */
    UNDEFINED
}