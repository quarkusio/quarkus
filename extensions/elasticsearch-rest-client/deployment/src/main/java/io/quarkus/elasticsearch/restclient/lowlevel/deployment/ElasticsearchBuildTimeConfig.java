package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.elasticsearch")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ElasticsearchBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();
}
