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
import org.junit.jupiter.api.condition.DisabledIf;
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

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-relaxed-no-groups-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class HybridKeyExchangeRelaxedNoGroupsTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/relaxed-no-groups", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-relaxed-no-groups-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-relaxed-no-groups-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "relaxed")
            // no key-exchange-groups — server uses TLS defaults (classical groups only)
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void testClassicClientConnects() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("relaxed-no-groups-ok");
        } finally {
            client.close();
        }
    }

    @Test
    @EnabledIf("isOpenSsl35Available")
    //if jdk is 27 or later, server will have pqc groups by default
    @DisabledIf("isJdk27OrLater")
    void testPqcClientFails() {
        // Server uses classical TLS defaults — no PQC groups advertised.
        // A client that only offers X25519MLKEM768 has no common group and the handshake fails.
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));
        options.setTrustAll(true);

        WebClient client = WebClient.create(vertx, options);
        try {
            assertThatThrownBy(() -> client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join())
                    .hasRootCauseInstanceOf(SSLException.class);
        } finally {
            client.close();
        }
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/relaxed-no-groups").handler(rc -> rc.response().end("relaxed-no-groups-ok"));
        }

    }
}
