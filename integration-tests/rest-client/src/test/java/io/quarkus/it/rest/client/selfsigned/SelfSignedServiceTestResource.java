package io.quarkus.it.rest.client.selfsigned;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;

public class SelfSignedServiceTestResource implements QuarkusTestResourceLifecycleManager {

    private Vertx vertx = Vertx.vertx();
    private Map<String, String> originalProps = new HashMap<>();

    @Override
    public Map<String, String> start() {
        File file = new File("target/certs");
        file.mkdirs();
        // Generate self-signed certificate
        // We do not use the junit 5 plugin to avoid having to annotate all the tests to make sure the certs are
        // generated before the tests are run
        CertificateGenerator generator = new CertificateGenerator(file.toPath(), false);
        try {
            generator.generate(new CertificateRequest()
                    .withName("self-signed")
                    .withFormat(Format.PKCS12)
                    .withPassword("changeit")
                    .withDuration(Duration.ofDays(2))
                    .withCN("localhost"));

            generator.generate(new CertificateRequest()
                    .withName("fake-host")
                    .withFormat(Format.PKCS12)
                    .withPassword("changeit")
                    .withDuration(Duration.ofDays(2))
                    .withCN("fake-host.com"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath("target/certs/self-signed-keystore.p12")
                        .setPassword("changeit"));
        var server = vertx.createHttpServer(options)
                .requestHandler(req -> req.response().end("Hello self-signed!"))
                .listen(-2).toCompletionStage().toCompletableFuture().join();

        setProperty("javax.net.ssl.trustStore", "target/certs/self-signed-truststore.p12");
        setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        setProperty("javax.net.ssl.trustStorePassword", "changeit");

        List<String> list = Arrays.asList(
                "javax.net.ssl.trustStore", "target/certs/self-signed-truststore.p12",
                "javax.net.ssl.trustStoreType", "PKCS12",
                "javax.net.ssl.trustStorePassword", "changeit",

                "quarkus.rest-client.self-signed.url", "https://localhost:" + server.actualPort() + "/",
                "quarkus.rest-client.self-signed.trust-store", "target/certs/self-signed-truststore.p12",
                "quarkus.rest-client.self-signed.trust-store-password", "changeit",

                "self-signed.port", String.valueOf(server.actualPort()),

                "quarkus.tls.fake-host.trust-store.p12.path", "target/certs/fake-host-truststore.p12",
                "quarkus.tls.fake-host.trust-store.p12.password", "changeit"

        );
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            result.put(list.get(i++), list.get(i));
        }
        return result;
    }

    void setProperty(String k, String v) {
        originalProps.put(k, System.getProperty(k));
        System.setProperty(k, v);
    }

    void restoreProperty(String k) {
        String v = originalProps.get(k);
        if (v == null) {
            System.getProperties().remove(k);
        } else {
            System.setProperty(k, v);
        }
    }

    @Override
    public void stop() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
        originalProps.keySet().forEach(this::restoreProperty);
    }
}
