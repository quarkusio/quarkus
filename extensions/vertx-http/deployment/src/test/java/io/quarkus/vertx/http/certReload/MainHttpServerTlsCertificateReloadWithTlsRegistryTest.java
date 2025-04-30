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
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certificates", certificates = {
        @Certificate(name = "reload-A", formats = Format.PEM),
        @Certificate(name = "reload-B", formats = Format.PEM, duration = 365),
})
@DisabledOnOs(OS.WINDOWS)
public class MainHttpServerTlsCertificateReloadWithTlsRegistryTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL url;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyBean.class))
            .overrideConfigKey("quarkus.http.ssl.insecure-requests", "redirect")
            .overrideConfigKey("quarkus.http.ssl.certificate.reload-period", "30s")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", temp.getAbsolutePath() + "/tls.crt")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", temp.getAbsolutePath() + "/tls.key")
            .overrideConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    // Prepare a random directory to store the certificates.
                    temp.mkdirs();
                    Files.copy(new File("target/certificates/reload-A.crt").toPath(),
                            new File(temp, "/tls.crt").toPath());
                    Files.copy(new File("target/certificates/reload-A.key").toPath(),
                            new File(temp, "/tls.key").toPath());
                    Files.copy(new File("target/certificates/reload-A-ca.crt").toPath(),
                            new File(temp, "/ca.crt").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .setAfterAllCustomizer(() -> {
                try {
                    Files.deleteIfExists(new File(temp, "/tls.crt").toPath());
                    Files.deleteIfExists(new File(temp, "/tls.key").toPath());
                    Files.deleteIfExists(new File(temp, "/ca.crt").toPath());
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
                .setTrustOptions(new PemTrustOptions().addCertPath("target/certificates/reload-A-ca.crt"));

        String response1 = vertx.createHttpClient(options)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        // Update certs
        Files.copy(new File("target/certificates/reload-B.crt").toPath(),
                new File(certs, "/tls.crt").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/certificates/reload-B.key").toPath(),
                new File(certs, "/tls.key").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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
                .setTrustOptions(new PemTrustOptions().addCertPath("target/certificates/reload-B-ca.crt"));

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
