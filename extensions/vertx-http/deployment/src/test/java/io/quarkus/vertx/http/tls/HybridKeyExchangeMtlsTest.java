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
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-hybrid-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }, client = true))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeMtlsTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/hybrid-mtls", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/mtls-hybrid-test-keystore.jks"), "server-keystore.jks")
                    .addAsResource(new File("target/certs/mtls-hybrid-test-server-truststore.jks"),
                            "server-truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.tls.trust-store.jks.path", "server-truststore.jks")
            .overrideConfigKey("quarkus.tls.trust-store.jks.password", "secret")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "strict")
            .overrideConfigKey("quarkus.http.ssl.client-auth", "REQUIRED")
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "x25519mlkem768")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void testHybridKeyExchangeWithMtls() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));
        options.setTrustAll(true);
        options.setKeyCertOptions(new JksOptions()
                .setPath("target/certs/mtls-hybrid-test-client-keystore.jks")
                .setPassword("secret"));

        WebClient client = WebClient.create(vertx, options);
        HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                .send().toCompletionStage().toCompletableFuture().join();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("mtls-hybrid-ok");
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/hybrid-mtls").handler(rc -> {
                assertThat(rc.request().connection().isSsl()).isTrue();
                assertThat(rc.request().isSSL()).isTrue();
                assertThat(rc.request().connection().sslSession()).isNotNull();
                assertThat(rc.request().connection().sslSession().getProtocol()).isEqualTo("TLSv1.3");
                try {
                    assertThat(rc.request().connection().sslSession().getPeerCertificates()).isNotEmpty();
                } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
                    throw new RuntimeException(e);
                }
                rc.response().end("mtls-hybrid-ok");
            });
        }

    }
}
