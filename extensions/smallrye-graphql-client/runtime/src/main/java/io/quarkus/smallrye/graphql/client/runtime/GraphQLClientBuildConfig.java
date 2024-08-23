package io.quarkus.smallrye.graphql.client.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-graphql-client", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class GraphQLClientBuildConfig {

    /**
     * Configuration item to enable build-time scanning in Quarkus for generating typesafe GraphQL client models.
     * If true, build-time scanning is enabled. By default, it is true.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableBuildTimeScanning;
}
