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
 * Verify that SSL configuration through `quarkus.*` propeties is correctly taken into account
 * for GraphQL clients.
 */
public class GraphQLClientSSLConfigurationTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql-client.client1.url=https://localhost:8080\n" +
                                            "quarkus.smallrye-graphql-client.client1.key-store=classpath:my.keystore\n" +
                                            "quarkus.smallrye-graphql-client.client1.key-store-password=secret\n" +
                                            "quarkus.smallrye-graphql-client.client1.key-store-type=PKCS12\n" +
                                            "quarkus.smallrye-graphql-client.client1.trust-store=classpath:my.truststore\n" +
                                            "quarkus.smallrye-graphql-client.client1.trust-store-password=secret2\n" +
                                            "quarkus.smallrye-graphql-client.client1.trust-store-type=JKS\n"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void checkConfiguration() {
        GraphQLClientConfiguration config = GraphQLClientsConfiguration.getInstance().getClient("client1");
        assertEquals("classpath:my.keystore", config.getKeyStore());
        assertEquals("secret", config.getKeyStorePassword());
        assertEquals("PKCS12", config.getKeyStoreType());
        assertEquals("classpath:my.truststore", config.getTrustStore());
        assertEquals("secret2", config.getTrustStorePassword());
        assertEquals("JKS", config.getTrustStoreType());
    }
}
