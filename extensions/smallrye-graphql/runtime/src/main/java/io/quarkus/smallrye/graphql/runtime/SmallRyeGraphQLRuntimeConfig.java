package io.quarkus.smallrye.graphql.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-graphql", phase = ConfigPhase.RUN_TIME)
public class SmallRyeGraphQLRuntimeConfig {

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled if it is included (see {@code always-include}).
     */
    @ConfigItem(name = "ui.enable", defaultValue = "true")
    boolean enable;

}
