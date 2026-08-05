package io.quarkus.smallrye.graphql.client.deployment.ssl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.smallrye.graphql.client.runtime.SmallRyeGraphQLClientRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;

/**
 * Verifies that when a request-scoped GraphQL client is destroyed,
 * its HttpClient is removed from the TLS reload registry in
 * {@link SmallRyeGraphQLClientRecorder}, preventing a memory leak.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-reload", password = "password", formats = Format.PKCS12, client = true)
})
public class TypesafeGraphQLClientTlsRegistryCleanupTest {

    private static final int PORT = 63805;
    private static final SSLTestingTools TOOLS = new SSLTestingTools();
    private static HttpServer server;

    private static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    private static final String CONFIGURATION = """
            # No config - overridden in the test
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .add(new StringAsset(CONFIGURATION), "application.properties")
                            .addClasses(MyApi.class, SSLTestingTools.class))
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .overrideRuntimeConfigKey("quarkus.smallrye-graphql-client.my-client.tls-configuration-name", "my-tls-client")
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("quarkus.smallrye-graphql-client.my-client.url", "https://127.0.0.1:" + PORT)
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.trust-all", "true")
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-reload-client-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @GraphQLClientApi(configKey = "my-client")
    @RequestScoped
    private interface MyApi {
        @Query
        String getResult();
    }

    @Inject
    MyApi myApi;

    @BeforeAll
    static void setupServer() throws Exception {
        server = TOOLS.runServer("target/certs/test-reload-keystore.p12",
                "password", "target/certs/test-reload-server-truststore.p12",
                "password");
    }

    @Test
    void httpClientRemovedFromRegistryAfterRequestScopeEnds() throws Exception {
        List<HttpClient> clientsBefore = getRegisteredHttpClients("my-tls-client");
        int sizeBefore = clientsBefore.size();

        // First request context: creates a request-scoped client and its HttpClient
        Arc.container().requestContext().activate();
        try {
            myApi.getResult();
        } finally {
            Arc.container().requestContext().terminate();
        }
        // After context terminates, the HttpClient should have been cleaned up
        assertThat(getRegisteredHttpClients("my-tls-client")).hasSize(sizeBefore);

        // Second request context: creates another request-scoped client
        Arc.container().requestContext().activate();
        try {
            myApi.getResult();
        } finally {
            Arc.container().requestContext().terminate();
        }
        // Still the same size — no accumulation
        assertThat(getRegisteredHttpClients("my-tls-client")).hasSize(sizeBefore);
    }

    @SuppressWarnings("unchecked")
    private static List<HttpClient> getRegisteredHttpClients(String tlsConfigName) throws Exception {
        Field field = SmallRyeGraphQLClientRecorder.class.getDeclaredField("tlsConfigNameToVertxHttpClients");
        field.setAccessible(true);
        Map<String, List<HttpClient>> map = (Map<String, List<HttpClient>>) field.get(null);
        List<HttpClient> list = map.get(tlsConfigName);
        return list != null ? List.copyOf(list) : List.of();
    }

    @AfterAll
    static void closeServer() {
        server.close();
        TOOLS.close();
    }
}
