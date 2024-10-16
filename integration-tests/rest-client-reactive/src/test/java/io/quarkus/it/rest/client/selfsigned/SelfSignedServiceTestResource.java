package io.quarkus.it.rest.client.selfsigned;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;

public class SelfSignedServiceTestResource implements QuarkusTestResourceLifecycleManager {

    Vertx vertx = Vertx.vertx();

    @Override
    public Map<String, String> start() {
        File file = new File("target/certs");
        file.mkdirs();
        // Generate self-signed certificate
        // We do not use the junit 5 plugin to avoid having to annotate all the tests to make sure the certs are
        // generated before the tests are run
        CertificateGenerator generator = new CertificateGenerator(file.toPath(), false);
        CertificateRequest cr = new CertificateRequest()
                .withName("self-signed")
                .withFormat(Format.PKCS12)
                .withPassword("changeit")
                .withDuration(Duration.ofDays(2))
                .withCN("localhost");
        try {
            generator.generate(cr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath("target/certs/self-signed-keystore.p12")
                        .setPassword("changeit"));
        var server = vertx.createHttpServer(options)
                .requestHandler(req -> req.response().end("OK"))
                .listen(-2).toCompletionStage().toCompletableFuture().join();

        return Map.of(
                "quarkus.rest-client.self-signed.url", "https://localhost:" + server.actualPort() + "/",
                "quarkus.rest-client.self-signed.trust-store", "target/certs/self-signed-truststore.p12",
                "quarkus.rest-client.self-signed.trust-store-password", "changeit");
    }

    @Override
    public void stop() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }
}
