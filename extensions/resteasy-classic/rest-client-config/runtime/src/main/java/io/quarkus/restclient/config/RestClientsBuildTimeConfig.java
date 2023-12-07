package io.quarkus.restclient.config;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rest-client", phase = ConfigPhase.BUILD_TIME)
public class RestClientsBuildTimeConfig {

    /**
     * Configurations of REST client instances.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, RestClientBuildConfig> configs = Collections.emptyMap();
}
