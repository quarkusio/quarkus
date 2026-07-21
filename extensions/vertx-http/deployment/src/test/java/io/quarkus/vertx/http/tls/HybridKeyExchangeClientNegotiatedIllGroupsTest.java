package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.List;

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
 * Tests client-negotiated PQC enforcement with a non-PQC key-exchange-group.
 * {@code x25519} is a valid curve but is not in Vert.x's PQ_COMPLIANT_GROUPS list, so in
 * client-negotiated mode, Vert.x prepends all PQC-compliant groups, making the final list
 * {@code [X25519MLKEM768, SecP256r1MLKEM768, SecP384r1MLKEM1024, x25519]}.
 * Both PQC and classical clients succeed.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-cn-ill-groups-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeClientNegotiatedIllGroupsTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/hybrid", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-cn-ill-groups-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-cn-ill-groups-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "client-negotiated")
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "x25519")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void pqcClientSucceeds() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                .send().toCompletionStage().toCompletableFuture().join();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("hybrid-ok");
    }

    @Test
    void classicalClientSucceeds() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("x25519"));

        WebClient client = WebClient.create(vertx, options);
        HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                .send().toCompletionStage().toCompletableFuture().join();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("hybrid-ok");
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hybrid").handler(rc -> rc.response().end("hybrid-ok"));
        }
    }
}
