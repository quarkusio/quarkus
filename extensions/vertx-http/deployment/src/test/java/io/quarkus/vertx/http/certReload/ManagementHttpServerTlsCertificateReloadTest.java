package io.quarkus.vertx.http.certReload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.net.ssl.SSLHandshakeException;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.options.TlsCertificateReloader;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certificates", certificates = {
        @Certificate(name = "reload-C", formats = Format.PEM),
        @Certificate(name = "reload-D", formats = Format.PEM, duration = 365),
})
@DisabledOnOs(OS.WINDOWS)
public class ManagementHttpServerTlsCertificateReloadTest {

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    private static final String APP_PROPS = """
            quarkus.management.enabled=true
            quarkus.management.ssl.certificate.reload-period=30s
            quarkus.management.ssl.certificate.files=%s
            quarkus.management.ssl.certificate.key-files=%s

            loc=%s
            """.formatted(temp.getAbsolutePath() + "/tls.crt", temp.getAbsolutePath() + "/tls.key", temp.getAbsolutePath());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"))
            .setBeforeAllCustomizer(() -> {
                try {
                    // Prepare a random directory to store the certificates.
                    temp.mkdirs();
                    Files.copy(new File("target/certificates/reload-C.crt").toPath(),
                            new File(temp, "/tls.crt").toPath());
                    Files.copy(new File("target/certificates/reload-C.key").toPath(),
                            new File(temp, "/tls.key").toPath());
                    Files.copy(new File("target/certificates/reload-C-ca.crt").toPath(),
                            new File(temp, "/ca.crt").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .addBuildChainCustomizer(buildCustomizer())
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

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                        context.produce(buildItem.routeBuilder()
                                .management()
                                .route("/hello")
                                .handler(new MyHandler())
                                .build());
                    }
                }).produces(RouteBuildItem.class)
                        .consumes(NonApplicationRootPathBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void test() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var options = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(9001) // Management interface test port
                .setDefaultHost("localhost")
                .setTrustOptions(new PemTrustOptions().addCertPath(new File(certs, "/ca.crt").getAbsolutePath()));

        String response1 = vertx.createHttpClient(options)
                .request(HttpMethod.GET, "/q/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        // Update certs
        Files.copy(new File("target/certificates/reload-D.crt").toPath(),
                new File(certs, "/tls.crt").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/certificates/reload-D.key").toPath(),
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
                .setTrustOptions(new PemTrustOptions().addCertPath("target/certificates/reload-D-ca.crt"));

        var response2 = vertx.createHttpClient(options2)
                .request(HttpMethod.GET, "/hello")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();

        assertThat(response1).isNotEqualTo(response2); // Because cert duration are different.
    }

    public static class MyHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            var exp = ((X509Certificate) rc.request().connection().sslSession().getLocalCertificates()[0]).getNotAfter()
                    .toInstant().toEpochMilli();
            rc.response().end("Hello Management " + exp);
        }
    }

}
