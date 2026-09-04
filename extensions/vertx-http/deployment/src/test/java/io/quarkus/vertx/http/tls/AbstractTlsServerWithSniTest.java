package io.quarkus.vertx.http.tls;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

abstract class AbstractTlsServerWithSniTest {

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @Inject
    Vertx vertx;

    abstract String expectedCertCn();

    @Test
    void testSni() throws TimeoutException {
        String[] parts = makeSniRequest();
        Assertions.assertThat(parts[0]).isEqualTo("localhost");
        Assertions.assertThat(parts[1]).contains(expectedCertCn());
    }

    private String[] makeSniRequest() throws TimeoutException {
        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setForceSni(true);
        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm()).send().await(4, TimeUnit.SECONDS);
            Assertions.assertThat(response.statusCode()).isEqualTo(200);
            return response.bodyAsString().split("\\|");
        } finally {
            client.close();
        }
    }

    @ApplicationScoped
    static class MyBean {

        void register(@Observes Router router) {
            router.get("/tls").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();

                String indicatedServerName = rc.request().connection().indicatedServerName();
                String certCn;
                try {
                    X509Certificate certificate = (X509Certificate) rc.request().connection().sslSession()
                            .getLocalCertificates()[0];
                    certCn = certificate.getSubjectX500Principal().getName();
                } catch (Exception e) {
                    certCn = "ERROR";
                }
                rc.response().end(indicatedServerName + "|" + certCn);
            });
        }
    }
}
