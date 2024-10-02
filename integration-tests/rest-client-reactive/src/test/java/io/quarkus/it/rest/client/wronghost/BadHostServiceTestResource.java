package io.quarkus.it.rest.client.wronghost;

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

public class BadHostServiceTestResource implements QuarkusTestResourceLifecycleManager {

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
                .withName("bad-host")
                .withFormat(Format.PKCS12)
                .withPassword("changeit")
                .withDuration(Duration.ofDays(2))
                .withCN("bad-host.com")
                .withSubjectAlternativeName("DNS:bad-host.com");
        try {
            generator.generate(cr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File f = new File("target/certs/bad-host-keystore.p12");
        System.out.println(f.getAbsolutePath() + " / " + f.exists());
        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath("target/certs/bad-host-keystore.p12")
                        .setPassword("changeit"));
        var server = vertx.createHttpServer(options)
                .requestHandler(req -> req.response().end("OK"))
                .listen(-1).toCompletionStage().toCompletableFuture().join();

        return Map.of(
                // Wrong Host client (connection accepted, as host verification is turned off)
                "quarkus.rest-client.wrong-host.url", "https://localhost:" + server.actualPort() + "/",
                "quarkus.rest-client.wrong-host.trust-store", "target/certs/bad-host-truststore.p12",
                "quarkus.rest-client.wrong-host.trust-store-password", "changeit",
                "quarkus.rest-client.wrong-host.verify-host", "false",

                // Wrong Host client verified (connection rejected, as host verification is turned on by default)
                "quarkus.rest-client.wrong-host-rejected.url", "https://localhost:" + server.actualPort() + "/",
                "quarkus.rest-client.wrong-host-rejected.trust-store", "target/certs/bad-host-truststore.p12",
                "quarkus.rest-client.wrong-host-rejected.trust-store-password", "changeit");
    }

    @Override
    public void stop() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }
}
