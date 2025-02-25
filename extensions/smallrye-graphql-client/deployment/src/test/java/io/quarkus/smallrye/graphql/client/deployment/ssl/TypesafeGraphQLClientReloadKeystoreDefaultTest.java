package io.quarkus.smallrye.graphql.client.deployment.ssl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.KeyCertOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "wrong-test-reload", password = "password", formats = Format.PKCS12, client = true),
        @Certificate(name = "test-reload", password = "password", formats = Format.PKCS12, client = true)
})
public class TypesafeGraphQLClientReloadKeystoreDefaultTest {

    private static final int PORT = 63805;
    private static final SSLTestingTools TOOLS = new SSLTestingTools();
    private static final String EXPECTED_RESPONSE = "HelloWorld";
    private static HttpServer server;

    private static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    private static final String CONFIGURATION = """
            # No config - overridden in the test
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .add(new StringAsset(CONFIGURATION), "application.properties")
                            .addClasses(MyApi.class, SSLTestingTools.class))
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("quarkus.smallrye-graphql-client.my-client.url", "https://127.0.0.1:" + PORT)
            .overrideRuntimeConfigKey("quarkus.tls.trust-all", "true")
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/wrong-test-reload-client-keystore.p12").toPath(),
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

    @Inject
    TlsConfigurationRegistry registry;

    @ConfigProperty(name = "loc")
    File certs;

    @Inject
    Event<CertificateUpdatedEvent> event;

    @BeforeAll
    static void setupServer() throws Exception {
        server = TOOLS.runServer("target/certs/test-reload-keystore.p12",
                "password", "target/certs/test-reload-server-truststore.p12",
                "password");
    }

    @Test
    void testReloading() throws IOException {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        KeyCertOptions keystoreOptionsBefore = (KeyCertOptions) GraphQLClientsConfiguration.getInstance().getClient("my-client")
                .getTlsKeyStoreOptions();
        Arc.container().requestContext().activate();
        try {
            myApi.getResult();
            Assertions.fail(); // should fail
        } catch (Exception ex) {
            assertHasCauseContainingMessage(ex, "Received fatal alert: certificate_unknown");
        } finally {
            Arc.container().requestContext().terminate();
        }
        Files.copy(new File("target/certs/test-reload-client-keystore.p12").toPath(),
                new File(certs, "/tls.p12").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertThat(def.reload()).isTrue();
        event.fire(new CertificateUpdatedEvent("<default>", def));
        Arc.container().requestContext().activate();
        try {
            assertThat(GraphQLClientsConfiguration.getInstance().getClient("my-client").getTlsKeyStoreOptions())
                    .isNotEqualTo(keystoreOptionsBefore);
            assertThat(myApi.getResult()).isEqualTo(EXPECTED_RESPONSE);
        } finally {
            Arc.container().requestContext().terminate();
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
                return;
            }
        }
        throw new RuntimeException("Unexpected exception", t);
    }
}
