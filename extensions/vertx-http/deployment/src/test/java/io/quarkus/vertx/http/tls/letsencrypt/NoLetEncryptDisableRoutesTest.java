package io.quarkus.vertx.http.tls.letsencrypt;

import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;

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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Checks that no routes are exposed if Let's Encrypt is not enabled (default).
 */
@Certificates(baseDir = "target/certs/lets-encrypt", certificates = {
        @Certificate(name = "self-signed", formats = { Format.PEM }), // Initial certificate
        @Certificate(name = "acme", formats = { Format.PEM }, duration = 365) // ACME certificate (fake), unused in this test
})
@DisabledOnOs(OS.WINDOWS)
public class NoLetEncryptDisableRoutesTest {
    private static final File SELF_SIGNED_CERT = new File("target/certs/lets-encrypt/self-signed.crt");
    private static final File SELF_SIGNED_KEY = new File("target/certs/lets-encrypt/self-signed.key");
    private static final File SELF_SIGNED_CA = new File("target/certs/lets-encrypt/self-signed-ca.crt");

    private static final String configuration = """
            # Enable SSL, configure the key store using the self-signed certificate
            quarkus.tls.key-store.pem.0.cert=%s
            quarkus.tls.key-store.pem.0.key=%s
            # Let's encrypt not enabled on purpose
            quarkus.http.insecure-requests=disabled
            """.formatted(SELF_SIGNED_CERT, SELF_SIGNED_KEY);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset((configuration)), "application.properties"));

    @Inject
    Vertx vertx;

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @TestHTTPResource(value = "/q/lets-encrypt/challenge", tls = true)
    String management;

    @TestHTTPResource(value = "/q/lets-encrypt/certs", tls = true)
    String reload;

    @TestHTTPResource(value = "/.well-known/acme-challenge/whatever", tls = true)
    String challenge;

    @Test
    void verifyNoLetsEncryptRouteExposedIfDisabled() {
        WebClientOptions options = new WebClientOptions().setSsl(true)
                .setTrustOptions(new PemTrustOptions().addCertPath(SELF_SIGNED_CA.getAbsolutePath()));
        WebClient client = WebClient.create(vertx, options);

        //  Verify the application is serving the application
        HttpResponse<Buffer> response = await(client.getAbs(url.toExternalForm()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);

        // No management route
        response = await(client.getAbs(management).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
        response = await(client.postAbs(management).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);
        response = await(client.deleteAbs(management).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);

        // No well-known route
        response = await(client.getAbs(challenge).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);

        // No reload route
        response = await(client.postAbs(reload).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);
    }

    private <T> T await(Future<T> future) {
        return future.toCompletionStage().toCompletableFuture().join();
    }

    @ApplicationScoped
    public static class MyBean {
        public void register(@Observes Router router) {
            router.get("/tls").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
                var exp = ((X509Certificate) rc.request().connection().sslSession().getLocalCertificates()[0])
                        .getNotAfter().toInstant().toEpochMilli();
                rc.response().end("expiration: " + exp);
            });
        }
    }
}
