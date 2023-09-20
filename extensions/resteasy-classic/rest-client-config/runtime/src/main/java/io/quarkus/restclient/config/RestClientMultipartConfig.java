package io.quarkus.restclient.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientMultipartConfig {

    /**
     * The max HTTP chunk size (8096 bytes by default).
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Integer> maxChunkSize;

}
