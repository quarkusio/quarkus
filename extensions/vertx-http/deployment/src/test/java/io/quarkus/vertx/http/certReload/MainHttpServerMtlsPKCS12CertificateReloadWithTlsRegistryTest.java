package io.quarkus.vertx.http.certReload;

import static io.quarkus.vertx.http.certReload.CertReloadTestHelper.assertTlsFails;
import static io.quarkus.vertx.http.certReload.CertReloadTestHelper.httpsGet;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.options.TlsCertificateReloader;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certificates", certificates = {
        @Certificate(name = "mtls-registry-reload", formats = Format.PKCS12, password = "password", client = true),
})
@DisabledOnOs(OS.WINDOWS)
class MainHttpServerMtlsPKCS12CertificateReloadWithTlsRegistryTest {

    private static final String CERTS_DIR = "target/certificates/";
    private static final String CERT_NAME = "mtls-registry-reload";
    private static final String PASSWORD = "password";

    @TestHTTPResource(value = "/hello", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyBean.class, CertReloadTestHelper.class))
            .overrideConfigKey("quarkus.http.tls-configuration-name", "http")
            .overrideConfigKey("quarkus.http.ssl.certificate.reload-period", "30s")
            .overrideConfigKey("quarkus.tls.http.key-store.p12.path", CERTS_DIR + CERT_NAME + "-keystore.p12")
            .overrideConfigKey("quarkus.tls.http.key-store.p12.password", PASSWORD)
            .overrideConfigKey("quarkus.tls.http.trust-store.p12.path", CERTS_DIR + CERT_NAME + "-server-truststore.p12")
            .overrideConfigKey("quarkus.tls.http.trust-store.p12.password", PASSWORD)
            .overrideConfigKey("quarkus.http.ssl.client-auth", "required");

    @Inject
    Vertx vertx;

    @Test
    void mTlsSurvivesReload() throws Exception {
        var withClientCert = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(url.getPort())
                .setDefaultHost(url.getHost())
                .setKeyCertOptions(
                        new PfxOptions().setPath(CERTS_DIR + CERT_NAME + "-client-keystore.p12").setPassword(PASSWORD))
                .setTrustOptions(
                        new PfxOptions().setPath(CERTS_DIR + CERT_NAME + "-client-truststore.p12").setPassword(PASSWORD));

        var withoutClientCert = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(url.getPort())
                .setDefaultHost(url.getHost())
                .setTrustOptions(
                        new PfxOptions().setPath(CERTS_DIR + CERT_NAME + "-client-truststore.p12").setPassword(PASSWORD));

        assertThat(httpsGet(vertx, withClientCert, "/hello")).startsWith("Hello");

        assertTlsFails(vertx, withoutClientCert, "/hello");

        TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertTlsFails(vertx, withoutClientCert, "/hello");

        assertThat(httpsGet(vertx, withClientCert, "/hello")).startsWith("Hello");
    }

    static class MyBean {
        void onStart(@Observes Router router) {
            router.get("/hello").handler(rc -> {
                var exp = ((X509Certificate) rc.request().connection().sslSession().getLocalCertificates()[0])
                        .getNotAfter().toInstant().toEpochMilli();
                rc.response().end("Hello " + exp);
            });
        }
    }
}
