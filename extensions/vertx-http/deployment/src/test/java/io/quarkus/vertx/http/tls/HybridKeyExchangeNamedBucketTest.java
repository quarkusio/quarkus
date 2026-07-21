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
 * Smoke test for PQC enforcement with a named TLS bucket. The policy/groups logic is already
 * thoroughly covered by the default bucket tests — this just verifies the named bucket code path
 * is wired correctly end-to-end.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-named-bucket-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeNamedBucketTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/named-bucket", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-named-bucket-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-named-bucket-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.pqc.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.pqc.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc.pqc-enforcement-policy", "strict")
            .overrideConfigKey("quarkus.tls.pqc.key-exchange-groups", "X25519MLKEM768")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "pqc")
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
            assertThat(response.bodyAsString()).isEqualTo("named-bucket-ok");
        } finally {
            client.close();
        }
    }

    @Test
    void testNonHybridClientRejected() {
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
            router.get("/named-bucket").handler(rc -> rc.response().end("named-bucket-ok"));
        }

    }
}
