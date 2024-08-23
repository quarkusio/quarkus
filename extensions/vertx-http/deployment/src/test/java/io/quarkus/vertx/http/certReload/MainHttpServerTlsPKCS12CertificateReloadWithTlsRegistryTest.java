package io.quarkus.vertx.http.certReload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.options.TlsCertificateReloader;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certificates", certificates = {
        @Certificate(name = "reload-A", formats = Format.PKCS12, password = "password"),
        @Certificate(name = "reload-B", formats = Format.PKCS12, password = "password", duration = 365),
})
@DisabledOnOs(OS.WINDOWS)
public class MainHttpServerTlsPKCS12CertificateReloadWithTlsRegistryTest {

    @TestHTTPResource(value = "/hello", ssl = true)
    URL url;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyBean.class))
            .overrideConfigKey("quarkus.http.insecure-requests", "redirect")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "http")
            .overrideConfigKey("quarkus.http.ssl.certificate.reload-period", "30s")

            .overrideConfigKey("quarkus.tls.http.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideConfigKey("quarkus.tls.http.key-store.p12.password", "password")

            .overrideConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    // Prepare a random directory to store the certificates.
                    temp.mkdirs();
                    Files.copy(new File("target/certificates/reload-A-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .setAfterAllCustomizer(() -> {
                try {
                    Files.deleteIfExists(new File(temp, "/tls.p12").toPath());
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void test() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var options = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(url.getPort())
                .setDefaultHost(url.getHost())
                .setTrustOptions(
                        new PfxOptions().setPath("target/certificates/reload-A-truststore.p12").setPassword("password"));

        String response1 = vertx.createHttpClient(options)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        // Update certs
        Files.copy(new File("target/certificates/reload-B-keystore.p12").toPath(),
                new File(certs, "/tls.p12").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Trigger the reload
        TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);

        // The client truststore is not updated, thus it should fail.
        assertThatThrownBy(() -> vertx.createHttpClient(options)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join()).hasCauseInstanceOf(SSLHandshakeException.class);

        var options2 = new HttpClientOptions(options)
                .setTrustOptions(
                        new PfxOptions().setPath("target/certificates/reload-B-truststore.p12").setPassword("password"));

        var response2 = vertx.createHttpClient(options2)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        assertThat(response1).isNotEqualTo(response2); // Because cert duration are different.

        // Trigger another reload
        TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);

        var response3 = vertx.createHttpClient(options2)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        assertThat(response2).isEqualTo(response3);
    }

    public static class MyBean {

        public void onStart(@Observes Router router) {
            router.get("/hello").handler(rc -> {
                var exp = ((X509Certificate) rc.request().connection().sslSession().getLocalCertificates()[0]).getNotAfter()
                        .toInstant().toEpochMilli();
                rc.response().end("Hello " + exp);
            });
        }

    }

}
