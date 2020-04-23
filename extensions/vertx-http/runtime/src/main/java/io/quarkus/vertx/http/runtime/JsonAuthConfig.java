package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * config for the REST JSON authentication mechanism
 */
@ConfigGroup
public class JsonAuthConfig {
    /**
     * If JSON authentication is enabled
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The login page
     */
    @ConfigItem(defaultValue = "/login")
    public String postLocation;

}
