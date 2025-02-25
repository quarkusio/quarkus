package io.quarkus.smallrye.graphql.client.deployment.ssl;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.vertx.core.http.HttpServer;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "graphql", password = "password", formats = { Format.PKCS12 }, client = true),
        @Certificate(name = "wrong-graphql", password = "wrong-password", formats = { Format.PKCS12 })
})
public class TypesafeGraphQLClientServerAuthenticationBadKeystoreOnServerTest {

    private static final int PORT = 63805;
    private static final SSLTestingTools TOOLS = new SSLTestingTools();
    private static HttpServer server;

    private static final String CONFIGURATION = """
                quarkus.smallrye-graphql-client.my-client.tls-configuration-name=my-tls-client
                quarkus.tls.my-tls-client.trust-store.p12.path=target/certs/graphql-client-truststore.p12
                quarkus.tls.my-tls-client.trust-store.p12.password=password
                quarkus.smallrye-graphql-client.my-client.url=https://127.0.0.1:%d/
            """.formatted(PORT);

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyApi.class, SSLTestingTools.class)
                    .addAsResource(new StringAsset(CONFIGURATION),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @GraphQLClientApi(configKey = "my-client")
    private interface MyApi {
        @Query
        String getResult();
    }

    @Inject
    MyApi myApi;

    @BeforeAll
    static void setupServer() throws Exception {
        server = TOOLS.runServer("target/certs/wrong-graphql-keystore.p12",
                "wrong-password", null, null);
    }

    @Test
    void serverAuthentication_badKeystoreOnServer() {
        try {
            myApi.getResult();
            Assertions.fail("Should not be able to connect");
        } catch (Exception ex) {
            assertHasCauseContainingMessage(ex, "Path does not chain with any of the trust anchors");
        }
    }

    @AfterAll
    static void closeServer() {
        server.close();
        TOOLS.close();
    }

    private void assertHasCauseContainingMessage(Throwable t, String message) {
        Throwable throwable = t;
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
            if (throwable.getMessage().contains(message)) {
                t.printStackTrace();
                return;
            }
        }
        throw new RuntimeException("Unexpected exception", t);
    }
}
