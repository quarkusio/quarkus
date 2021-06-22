package io.quarkus.mongodb.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mongodb", phase = ConfigPhase.BUILD_TIME)
public class MongoClientBuildTimeConfig {
    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Whether or not metrics are published in case a metrics extension is present.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * Whether or not tracing spans of driver commands are sent in case the smallrye-opentracing extension is present.
     */
    @ConfigItem(name = "tracing.enabled")
    public boolean tracingEnabled;

    /**
     * If set to true, the default clients will always be created even if there are no injection points that use them
     * 
     * @deprecated use forceDefaultSyncClient and/or forceDefaultReactiveClient instead
     */
    @ConfigItem(name = "force-default-clients")
    @Deprecated
    public boolean forceDefaultClients;

    /**
     * If set to true, the default sync client will always be created even if there are no injection points that use it
     */
    @ConfigItem
    public boolean forceDefaultSyncClient;

    /**
     * If set to true, the default reactive client will always be created even if there are no injection points that use it
     */
    @ConfigItem
    public boolean forceDefaultReactiveClient;

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start MongoDB in dev and test mode.
     */
    @ConfigItem
    public DevServicesBuildTimeConfig devservices;
}
