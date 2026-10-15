package io.quarkus.it.rest.client.trustall;

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

    private Vertx vertx;
    private boolean createdVertx;

    @Override
    public Map<String, String> start() {
        // Reuse the ambient Vert.x instance if there is one, otherwise create (and later close) our own
        var context = Vertx.currentContext();
        if (context != null) {
            vertx = context.owner();
        } else {
            vertx = Vertx.vertx();
            createdVertx = true;
        }
        File file = new File("target/certs");
        file.mkdirs();
        // Generate self-signed certificate
        // We do not use the JUnit plugin to avoid having to annotate all the tests to make sure the certs are
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

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath("target/certs/bad-host-keystore.p12")
                        .setPassword("changeit"));
        var server = vertx.createHttpServer(options)
                .requestHandler(req -> req.response().end("OK"))
                .listen(0).await();

        return Map.of("wrong-host/mp-rest/url", "https://localhost:" + server.actualPort() + "/");
    }

    @Override
    public void stop() {
        if (createdVertx) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
