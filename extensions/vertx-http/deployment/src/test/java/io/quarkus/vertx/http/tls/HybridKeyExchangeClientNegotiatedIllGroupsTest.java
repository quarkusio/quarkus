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

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-client-neg-ill-groups-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeClientNegotiatedIllGroupsTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/client-neg-ill-groups", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-client-neg-ill-groups-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-client-neg-ill-groups-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "client-negotiated")
            // PQC-only group — unlike the no-groups case, explicitly-set groups are not overridden;
            // classical clients will fail because X25519 is not in the advertised group list
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "X25519MLKEM768")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void testPqcClientConnects() {
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
            assertThat(response.bodyAsString()).isEqualTo("client-neg-ill-groups-ok");
        } finally {
            client.close();
        }
    }

    @Test
    void testClassicClientRejectedWhenGroupsExplicitlyPqcOnly() {
        // Explicitly-set groups are not overridden — X25519 is absent from the server's group list,
        // so a non-PQC client has no common group and the handshake fails.
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

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/client-neg-ill-groups").handler(rc -> rc.response().end("client-neg-ill-groups-ok"));
        }

    }
}
