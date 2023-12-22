package io.quarkus.restclient.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientLoggingConfig {
    /**
     * Scope of logging for the client.
     * <br/>
     * WARNING: beware of logging sensitive data
     * <br/>
     * The possible values are:
     * <ul>
     * <li>{@code request-response} - enables logging request and responses, including redirect responses</li>
     * <li>{@code all} - enables logging requests and responses and lower-level logging</li>
     * <li>{@code none} - no additional logging</li>
     * </ul>
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> scope;

    /**
     * How many characters of the body should be logged. Message body can be large and can easily pollute the logs.
     * <p>
     * By default, set to 100.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "100")
    public Integer bodyLimit;
}
