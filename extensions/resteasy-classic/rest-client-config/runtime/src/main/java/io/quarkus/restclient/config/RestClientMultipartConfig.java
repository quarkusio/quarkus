package io.quarkus.restclient.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientMultipartConfig {

    /**
     * The max HTTP chunk size (8096 bytes by default).
     * <p>
     * This property is applicable to reactive REST clients only.
     *
     * @Deprecated Use {@code quarkus.rest-client.max-chunk-size} instead
     */
    @Deprecated
    @ConfigItem
    public Optional<Integer> maxChunkSize;

}
