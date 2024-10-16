package io.quarkus.smallrye.graphql.client.deployment.ssl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.vertx.core.http.HttpServer;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "graphql", password = "password", formats = {
        Format.PKCS12 }, client = true))
public class TypesafeGraphQLClientServerAuthenticationCorrectTruststoreTest {

    private static final int PORT = 63805;
    private static final SSLTestingTools TOOLS = new SSLTestingTools();
    private static final String EXPECTED_RESPONSE = "HelloWorld";
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
        server = TOOLS.runServer("target/certs/graphql-keystore.p12",
                "password", null, null);
    }

    @Test
    void serverAuthentication_correctTruststore() {
        assertThat(myApi.getResult()).isEqualTo(EXPECTED_RESPONSE);
    }

    @AfterAll
    static void closeServer() {
        server.close();
        TOOLS.close();
    }
}
