package io.quarkus.vertx.http.runtime.options;

import java.util.OptionalInt;

import io.netty.handler.codec.compression.Brotli;
import io.vertx.core.http.CompressionConfig;

/**
 * Isolates the calls that need Brotli4j, which is an optional dependency, so that they can be cut from a native
 * executable built without it. See {@code BrotliCompressionSupportSubstitutions}.
 */
final class BrotliCompressionSupport {

    private BrotliCompressionSupport() {
    }

    static void addBrotli(CompressionConfig compression, OptionalInt compressionLevel) {
        // Netty silently ignores Brotli when its native library cannot be loaded, so check it first.
        try {
            Brotli.ensureAvailability();
        } catch (Throwable t) {
            throw new IllegalStateException(
                    "quarkus.http.compressors contains 'br' but the Brotli4j native library could not be loaded."
                            + " Make sure the com.aayushatharva.brotli4j:native-<os>-<arch> artifact matching the"
                            + " current platform is available.",
                    t);
        }
        if (compressionLevel.isPresent()) {
            compression.addBrotli(compressionLevel.getAsInt());
        } else {
            compression.addBrotli();
        }
    }
}
