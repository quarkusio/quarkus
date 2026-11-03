package io.quarkus.vertx.http.tls.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Checks that the Let's Encrypt client of the application serves a self-signed certificate for its domains until
 * the first order completes, and that it does not expose the management routes of the Quarkus CLI flow.
 */
public class LetsEncryptClientPlaceholderTest {

    private static final String configuration = """
            quarkus.tls.lets-encrypt.client.enabled=true
            quarkus.tls.lets-encrypt.client.domains=localhost,www.localhost
            quarkus.tls.lets-encrypt.client.terms-of-service-agreed=true
            # no ACME server listens there: the order fails and the application keeps the self-signed certificate
            quarkus.tls.lets-encrypt.client.directory-url=http://127.0.0.1:1/directory
            quarkus.tls.lets-encrypt.client.storage-directory=target/lets-encrypt-client-placeholder
            quarkus.http.insecure-requests=enabled
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset(configuration), "application.properties"));

    @Inject
    Vertx vertx;

    @Inject
    TlsConfigurationRegistry registry;

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @TestHTTPResource(value = "/q/lets-encrypt/challenge")
    URL management;

    @Test
    void servesASelfSignedCertificateForTheDomains() throws Exception {
        TlsConfiguration configuration = registry.getDefault().orElseThrow();
        X509Certificate certificate = (X509Certificate) configuration.getKeyStore()
                .getCertificate(configuration.getKeyStore().aliases().nextElement());

        assertThat(certificate.getSubjectX500Principal()).isEqualTo(certificate.getIssuerX500Principal());
        assertThat(certificate.getSubjectAlternativeNames())
                .extracting(name -> (Object) name.get(1))
                .containsExactly("localhost", "www.localhost");

        WebClient client = WebClient.create(vertx, new WebClientOptions().setSsl(true).setTrustAll(true)
                .setVerifyHost(false));
        try {
            HttpResponse<?> response = client.getAbs(url.toExternalForm()).send().await(10, TimeUnit.SECONDS);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("Hello");
        } finally {
            client.close();
        }
    }

    @Test
    void doesNotExposeTheManagementRoutesOfTheCliFlow() throws Exception {
        WebClient client = WebClient.create(vertx);
        try {
            assertThat(client.getAbs(management.toExternalForm()).send().await(10, TimeUnit.SECONDS).statusCode())
                    .isEqualTo(404);
        } finally {
            client.close();
        }
    }

    @ApplicationScoped
    public static class MyBean {

        public void register(@Observes Router router) {
            router.get("/tls").handler(rc -> rc.response().end("Hello"));
        }
    }
}
