package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.net.ssl.SSLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Test for issue Quarkusio-56774: When STRICT PQC enforcement policy is set with mixed PQC and non-PQC
 * key exchange groups (e.g., X25519MLKEM768,X25519), the server should filter out non-PQC groups and
 * only accept connections from clients using the configured PQC groups.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-strict-mixed-groups-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeStrictMixedGroupsTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/strict-mixed-groups", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-strict-mixed-groups-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-strict-mixed-groups-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "strict")
            // Mixed PQC and classical groups — X25519 should be filtered out by STRICT policy
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "X25519MLKEM768,X25519")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void testPqcClientWithAdvertisedGroupConnects() {
        // Client using X25519MLKEM768 (which is in the configured list) should connect successfully
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("strict-mixed-groups-ok");
        } finally {
            client.close();
        }
    }

    @Test
    void testPqcClientWithUnadvertisedGroupFails() {
        // Client using SecP256r1MLKEM768 (not in the configured list) should fail
        // because STRICT policy filters the list to only X25519MLKEM768
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("SecP256r1MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            assertThatThrownBy(() -> client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join())
                    .hasRootCauseInstanceOf(SSLException.class);
        } finally {
            client.close();
        }
    }

    @Test
    void testClassicalOnlyClientRejected() {
        // Client using only X25519 (classical, filtered out by STRICT policy) should fail
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("x25519"));

        WebClient client = WebClient.create(vertx, options);
        try {
            assertThatThrownBy(() -> client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join())
                    .hasRootCauseInstanceOf(SSLException.class);
        } finally {
            client.close();
        }
    }

    @Test
    void testClientWithMixedGroupsFallsBackToAdvertisedPqc() {
        // Client prefers SecP256r1MLKEM768 (not advertised) but also supports X25519MLKEM768.
        // Should negotiate X25519MLKEM768 successfully.
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("SecP256r1MLKEM768", "X25519MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("strict-mixed-groups-ok");
        } finally {
            client.close();
        }
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/strict-mixed-groups").handler(rc -> rc.response().end("strict-mixed-groups-ok"));
        }

    }
}
