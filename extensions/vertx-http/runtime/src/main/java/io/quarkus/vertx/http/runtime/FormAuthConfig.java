package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * config for the form authentication mechanism
 */
@ConfigGroup
public class FormAuthConfig {

    /**
     * If form authentication is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The post location.
     */
    @ConfigItem(defaultValue = "/j_security_check")
    public String postLocation;

}
