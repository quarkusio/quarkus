package io.quarkus.vertx.http.runtime.management;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Authentication for the management interface.
 */
@ConfigGroup
public class ManagementAuthConfig {
    /**
     * If basic auth should be enabled.
     *
     */
    @ConfigItem
    public Optional<Boolean> basic;

    /**
     * If this is true and credentials are present then a user will always be authenticated
     * before the request progresses.
     *
     * If this is false then an attempt will only be made to authenticate the user if a permission
     * check is performed or the current user is required for some other reason.
     */
    @ConfigItem(defaultValue = "true")
    public boolean proactive;
}
