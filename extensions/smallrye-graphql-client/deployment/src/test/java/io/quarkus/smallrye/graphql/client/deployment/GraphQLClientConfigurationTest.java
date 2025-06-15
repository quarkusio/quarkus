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
 * Verify that various configuration through `quarkus.*` properties is correctly taken into account for GraphQL clients.
 */
public class GraphQLClientConfigurationTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(new StringAsset("quarkus.smallrye-graphql-client.client1.url=https://localhost:8080\n"
                    + "quarkus.smallrye-graphql-client.client1.key-store=classpath:my.keystore\n"
                    + "quarkus.smallrye-graphql-client.client1.key-store-password=secret\n"
                    + "quarkus.smallrye-graphql-client.client1.key-store-type=PKCS12\n"
                    + "quarkus.smallrye-graphql-client.client1.trust-store=classpath:my.truststore\n"
                    + "quarkus.smallrye-graphql-client.client1.trust-store-password=secret2\n"
                    + "quarkus.smallrye-graphql-client.client1.trust-store-type=JKS\n"
                    + "quarkus.smallrye-graphql-client.client1.proxy-host=myproxy\n"
                    + "quarkus.smallrye-graphql-client.client1.proxy-port=1234\n"
                    + "quarkus.smallrye-graphql-client.client1.proxy-username=dave\n"
                    + "quarkus.smallrye-graphql-client.client1.proxy-password=secret\n"
                    + "quarkus.smallrye-graphql-client.client1.max-redirects=6\n"), "application.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void checkSslConfiguration() {
        GraphQLClientConfiguration config = GraphQLClientsConfiguration.getInstance().getClient("client1");
        assertEquals("classpath:my.keystore", config.getKeyStore());
        assertEquals("secret", config.getKeyStorePassword());
        assertEquals("PKCS12", config.getKeyStoreType());
        assertEquals("classpath:my.truststore", config.getTrustStore());
        assertEquals("secret2", config.getTrustStorePassword());
        assertEquals("JKS", config.getTrustStoreType());
    }

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
