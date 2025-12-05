package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface ElasticsearchLowLevelClientBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    /**
     * This property is to help Quarkus identify any low-level Elasticsearch REST clients
     * that possibly are not explicitly injected in the application code with their corresponding identifier, e.g.
     * {@code @Inject @Identifier("client-name") RestClient client;}.
     * In such cases, this property is required to help Quarkus detect all the necessary clients at build time.
     * The value of this configuration property is ignored.
     */
    @WithDefault("true")
    boolean forceDiscovery();
}
