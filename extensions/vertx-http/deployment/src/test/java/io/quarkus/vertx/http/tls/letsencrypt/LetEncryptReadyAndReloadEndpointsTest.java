package io.quarkus.vertx.http.tls.letsencrypt;

import static io.quarkus.vertx.http.tls.letsencrypt.LetsEncryptFlowTestBase.SELF_SIGNED_CA;
import static io.quarkus.vertx.http.tls.letsencrypt.LetsEncryptFlowTestBase.SELF_SIGNED_CERT;
import static io.quarkus.vertx.http.tls.letsencrypt.LetsEncryptFlowTestBase.SELF_SIGNED_KEY;
import static io.quarkus.vertx.http.tls.letsencrypt.LetsEncryptFlowTestBase.await;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs/lets-encrypt", certificates = {
        @Certificate(name = "self-signed", formats = { Format.PEM }), // Initial certificate
        @Certificate(name = "acme", formats = { Format.PEM }, duration = 365) // ACME certificate (fake)
})
@DisabledOnOs(OS.WINDOWS)
public class LetEncryptReadyAndReloadEndpointsTest {

    private static final String configuration = """
            # Configuration foo is ready
            quarkus.tls.foo.key-store.pem.0.cert=%s
            quarkus.tls.foo.key-store.pem.0.key=%s

            # Default configuration is not ready
            quarkus.tls.trust-all=true

            # Configuration bar is not ready
            quarkus.tls.bar.trust-all=true

            quarkus.tls.lets-encrypt.enabled=true
            """.formatted(SELF_SIGNED_CERT, SELF_SIGNED_KEY);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, LetsEncryptFlowTestBase.class)
                    .addAsResource(new StringAsset((configuration)), "application.properties"));

    @Inject
    Vertx vertx;

    @TestHTTPResource(value = "/q/lets-encrypt/challenge")
    String management;

    @TestHTTPResource(value = "/q/lets-encrypt/certs")
    String reload;

    @TestHTTPResource(value = "/hello")
    String endpoint;

    @Test
    void verifyReadyConfiguration() {
        WebClientOptions options = new WebClientOptions().setSsl(true)
                .setTrustOptions(new PemTrustOptions().addCertPath(SELF_SIGNED_CA.getAbsolutePath()));
        WebClient client = WebClient.create(vertx, options);

        //  Verify the application is serving the application
        HttpResponse<Buffer> response = await(client.getAbs(endpoint).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);

        // Verify that the default configuration is not ready (no key store)
        response = await(client.getAbs(management).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(503);

        // Verify that the foo configuration is ready
        response = await(client.getAbs(management + "/?key=foo").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        // Verify that the bar configuration is not ready
        response = await(client.getAbs(management + "/?key=bar").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(503);

        // Verify that the missing configuration is not ready
        response = await(client.getAbs(management + "/?key=missing").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(503);
    }

    @Test
    void verifyReload() {
        WebClientOptions options = new WebClientOptions().setSsl(true)
                .setTrustOptions(new PemTrustOptions().addCertPath(SELF_SIGNED_CA.getAbsolutePath()));
        WebClient client = WebClient.create(vertx, options);
        // Reload default
        HttpResponse<Buffer> response = await(client.postAbs(reload).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(500); // Cannot reload certificate, none set (not ready)

        // Reload foo
        response = await(client.postAbs(reload + "/?key=foo").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        response = await(client.postAbs(reload + "/?key=bar").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(500); // Cannot reload certificate, none set (not ready)

        // Reload missing
        response = await(client.postAbs(reload + "/?key=missing").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
    }

    @ApplicationScoped
    public static class MyBean {

        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> {
                rc.response().end("hello");
            });
        }
    }

}
