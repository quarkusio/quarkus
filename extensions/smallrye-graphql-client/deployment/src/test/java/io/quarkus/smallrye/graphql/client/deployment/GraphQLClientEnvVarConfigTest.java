package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import graphql.AssertException;
import io.quarkus.smallrye.graphql.client.deployment.other.MyEnvSource;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientBuildConfig;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientConfig;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientsConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

public class GraphQLClientEnvVarConfigTest {

    private static final String URL = "http://localhost:8080/graphql";
    private static final String CONFIG_KEY = "key";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyGraphQLClient.class, MyEnvSource.class)
                    .addAsServiceProvider(ConfigSource.class, MyEnvSource.class));

    @Inject
    GraphQLClientsConfig config; // runtime

    @Inject
    GraphQLClientBuildConfig buildConfig; // buildtime

    @Test
    void testConfigFromEnvVar() {
        assertFalse(buildConfig.enableBuildTimeScanning());
        GraphQLClientConfig client = config.clients().get(CONFIG_KEY);
        assertNotNull(client);
        assertEquals(URL,
                client.url().orElseThrow(() -> new AssertException("URL not found in '%s' config".formatted(CONFIG_KEY))));
    }

    @GraphQLClientApi(configKey = CONFIG_KEY)
    public interface MyGraphQLClient {
        @Query
        String hello();
    }
}
