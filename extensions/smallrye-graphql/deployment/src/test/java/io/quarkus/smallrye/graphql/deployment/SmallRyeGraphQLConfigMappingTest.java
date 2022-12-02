package io.quarkus.smallrye.graphql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class SmallRyeGraphQLConfigMappingTest {
    @Test
    void graphQlRelocates() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("quarkus.smallrye-graphql.show-runtime-exception-message", "org.acme.CustomRuntimeException")
                .withInterceptors(new SmallRyeGraphQLConfigMapping())
                .build();

        assertEquals("org.acme.CustomRuntimeException", config.getRawValue("mp.graphql.showErrorMessage"));
    }

    @Test
    void graphQlRelocatesDiscovered() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("quarkus.smallrye-graphql.show-runtime-exception-message", "org.acme.CustomRuntimeException")
                .addDiscoveredInterceptors()
                .build();

        assertEquals("org.acme.CustomRuntimeException", config.getRawValue("mp.graphql.showErrorMessage"));
    }
}
