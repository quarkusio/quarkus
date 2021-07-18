package io.quarkus.elytron.security.runtime.config;

import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "security", phase = ConfigPhase.RUN_TIME)
public class ElytronRuntimeConfig {

    /**
     * The mappings from group to role
     */
    @ConfigItem
    public Map<String, Set<String>> groupToRole;
}
