package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PolicyConfig {

    /**
     * The roles that are allowed to access resources protected by this policy
     */
    @ConfigItem
    public List<String> rolesAllowed;
}
