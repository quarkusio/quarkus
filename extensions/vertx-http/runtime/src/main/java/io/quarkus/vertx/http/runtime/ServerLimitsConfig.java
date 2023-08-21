package io.quarkus.vertx.http.runtime;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.WithDefault;

public interface ServerLimitsConfig {
    /**
     * The maximum length of all headers.
     */
    @WithDefault("20K")
    MemorySize maxHeaderSize();

    /**
     * The maximum size of a request body.
     */
    @WithDefault("10240K")
    Optional<MemorySize> maxBodySize();

    /**
     * The max HTTP chunk size
     */
    @WithDefault("8192")
    MemorySize maxChunkSize();

    /**
     * The maximum length of the initial line (e.g. {@code "GET / HTTP/1.0"}).
     */
    @WithDefault("4096")
    int maxInitialLineLength();

    /**
     * The maximum length of a form attribute.
     */
    @WithDefault("2048")
    MemorySize maxFormAttributeSize();

    /**
     * The maximum number of connections that are allowed at any one time. If this is set
     * it is recommended to set a short idle timeout.
     */
    OptionalInt maxConnections();

    /**
     * Set the SETTINGS_HEADER_TABLE_SIZE HTTP/2 setting.
     * <p>
     * Allows the sender to inform the remote endpoint of the maximum size of the header compression table used to decode
     * header blocks, in octets. The encoder can select any size equal to or less than this value by using signaling
     * specific to the header compression format inside a header block.
     * The initial value is {@code 4,096} octets.
     */
    OptionalLong headerTableSize();

    /**
     * Set SETTINGS_MAX_CONCURRENT_STREAMS HTTP/2 setting.
     * <p>
     * Indicates the maximum number of concurrent streams that the sender will allow. This limit is directional: it
     * applies to the number of streams that the sender permits the receiver to create. Initially, there is no limit to
     * this value. It is recommended that this value be no smaller than 100, to not unnecessarily limit parallelism.
     */
    OptionalLong maxConcurrentStreams();

    /**
     * Set the SETTINGS_MAX_FRAME_SIZE HTTP/2 setting.
     * Indicates the size of the largest frame payload that the sender is willing to receive, in octets.
     * The initial value is {@code 2^14} (16,384) octets.
     */
    OptionalInt maxFrameSize();

    /**
     * Set the SETTINGS_MAX_HEADER_LIST_SIZE HTTP/2 setting.
     * This advisory setting informs a peer of the maximum size of header list that the sender is prepared to accept,
     * in octets. The value is based on the uncompressed size of header fields, including the length of the name and
     * value in octets plus an overhead of 32 octets for each header field.
     * The default value is {@code 8192}
     */
    OptionalLong maxHeaderListSize();
}
