package io.quarkus.rest.client.reactive.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "wrong-test-reload", password = "password", formats = Format.PKCS12, client = true),
        @Certificate(name = "test-reload", password = "password", formats = Format.PKCS12, client = true)
})
public class TlsConfigReloadKeystoreTest {

    private static final int PORT = 63806;
    private static final String EXPECTED_RESPONSE = "HelloWorld";
    private static HttpServer testServer;
    private static Vertx testVertx;

    private static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Client.class, SSLTools.class))
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .overrideRuntimeConfigKey("quarkus.rest-client.my-client.tls-configuration-name", "my-tls-client")
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("quarkus.rest-client.my-client.url", "https://127.0.0.1:" + PORT)
            .overrideRuntimeConfigKey("quarkus.tls.my-tls-client.trust-all", "true")
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/wrong-test-reload-client-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @RegisterRestClient(configKey = "my-client")
    private interface Client {
        @GET
        String getResult();
    }

    @RestClient
    Client client;

    @Inject
    TlsConfigurationRegistry registry;

    @ConfigProperty(name = "loc")
    File certs;

    @Inject
    Event<CertificateUpdatedEvent> event;

    @BeforeAll
    static void setupServer() throws Exception {
        testVertx = Vertx.vertx();
        testServer = runServer(testVertx, "target/certs/test-reload-keystore.p12",
                "password", "target/certs/test-reload-server-truststore.p12",
                "password");
    }

    @AfterAll
    static void closeServer() {
        testServer.close();
        testVertx.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    void testReloading() throws IOException {
        TlsConfiguration tlsClient = registry.get("my-tls-client").orElseThrow();
        try {
            client.getResult();
            Assertions.fail(); // should fail
        } catch (Exception ex) {
            assertHasCauseContainingMessage(ex, "Received fatal alert: certificate_unknown");
        }
        Files.copy(new File("target/certs/test-reload-client-keystore.p12").toPath(),
                new File(certs, "/tls.p12").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertThat(tlsClient.reload()).isTrue();
        event.fire(new CertificateUpdatedEvent("my-tls-client", tlsClient));
        assertThat(client.getResult()).isEqualTo(EXPECTED_RESPONSE);
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

    static HttpServer runServer(Vertx vertx, String keystorePath, String keystorePassword,
            String truststorePath, String truststorePassword)
            throws InterruptedException, ExecutionException, TimeoutException {
        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(true);
        options.setHost("localhost");

        if (keystorePath != null) {
            PfxOptions keystoreOptions = new PfxOptions();
            KeyStore keyStore = SSLTools.createKeyStore(keystorePath, "PKCS12", keystorePassword);
            keystoreOptions.setValue(SSLTools.asBuffer(keyStore, keystorePassword.toCharArray()));
            keystoreOptions.setPassword(keystorePassword);
            options.setKeyCertOptions(keystoreOptions);
        }

        if (truststorePath != null) {
            options.setClientAuth(ClientAuth.REQUIRED);
            PfxOptions truststoreOptions = new PfxOptions();
            KeyStore trustStore = SSLTools.createKeyStore(truststorePath, "PKCS12", truststorePassword);
            truststoreOptions.setValue(SSLTools.asBuffer(trustStore, truststorePassword.toCharArray()));
            truststoreOptions.setPassword(truststorePassword);
            options.setTrustOptions(truststoreOptions);
        }

        HttpServer server = vertx.createHttpServer(options);
        server.requestHandler(request -> {
            request.response().send("HelloWorld");
        });

        return server.listen(PORT).toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
}
