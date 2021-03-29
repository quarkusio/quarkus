package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class PolicyConfig {

    /**
     * The roles that are allowed to access resources protected by this policy
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public List<String> rolesAllowed;
}
