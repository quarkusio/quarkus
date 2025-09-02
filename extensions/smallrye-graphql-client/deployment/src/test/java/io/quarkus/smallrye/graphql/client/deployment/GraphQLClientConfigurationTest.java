package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;

/**
 * Verify that various configuration through `quarkus.*` properties is correctly taken into account
 * for GraphQL clients.
 */
public class GraphQLClientConfigurationTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql-client.client1.url=https://localhost:8080\n" +
                                            "quarkus.smallrye-graphql-client.client1.proxy-host=myproxy\n" +
                                            "quarkus.smallrye-graphql-client.client1.proxy-port=1234\n" +
                                            "quarkus.smallrye-graphql-client.client1.proxy-username=dave\n" +
                                            "quarkus.smallrye-graphql-client.client1.proxy-password=secret\n" +
                                            "quarkus.smallrye-graphql-client.client1.max-redirects=6\n"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void checkProxyConfiguration() {
        GraphQLClientConfiguration config = GraphQLClientsConfiguration.getInstance().getClient("client1");
        assertEquals("myproxy", config.getProxyHost());
        assertEquals(1234, config.getProxyPort());
        assertEquals("dave", config.getProxyUsername());
        assertEquals("secret", config.getProxyPassword());
    }

    @Test
    public void checkMaxRedirects() {
        GraphQLClientConfiguration config = GraphQLClientsConfiguration.getInstance().getClient("client1");
        assertEquals(6, config.getMaxRedirects());
    }
}
