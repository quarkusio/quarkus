package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FilterConfig {

    /**
     * A regular expression for the paths matching this configuration
     */
    @ConfigItem
    public String matches;

    /**
     * Additional HTTP Headers always sent in the response
     */
    @ConfigItem
    @ConfigDocMapKey("header-name")
    public Map<String, String> header;

    /**
     * The HTTP methods for this path configuration
     */
    @ConfigItem
    public Optional<List<String>> methods;

    /**
     * Order in which this path config is applied. Higher priority takes precedence
     */
    @ConfigItem
    public OptionalInt order;
}
