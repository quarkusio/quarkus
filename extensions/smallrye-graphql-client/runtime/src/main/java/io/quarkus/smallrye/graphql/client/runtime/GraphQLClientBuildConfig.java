package io.quarkus.smallrye.graphql.client.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.smallrye-graphql-client")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface GraphQLClientBuildConfig {

    /**
     * Configuration item to enable build-time scanning in Quarkus for generating typesafe GraphQL client models.
     * If true, build-time scanning is enabled. By default, it is true.
     */
    @WithDefault("true")
    boolean enableBuildTimeScanning();
}
