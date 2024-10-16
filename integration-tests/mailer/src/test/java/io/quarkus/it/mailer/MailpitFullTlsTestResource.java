package io.quarkus.it.mailer;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

public class MailpitFullTlsTestResource implements QuarkusTestResourceLifecycleManager {

    public GenericContainer<?> server = new GenericContainer<>("axllent/mailpit")
            .withExposedPorts(8025, 1025);

    static {
        CertificateRequest request = new CertificateRequest()
                .withName("mailpit")
                .withFormat(Format.PEM);
        try {
            File dir = new File("target/certs");
            dir.mkdirs();
            new CertificateGenerator(dir.toPath(), false).generate(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> start() {
        server = server.withCopyFileToContainer(
                MountableFile.forHostPath("target/certs"),
                "/certs")
                .withEnv("MP_SMTP_TLS_CERT", "/certs/mailpit.crt")
                .withEnv("MP_SMTP_TLS_KEY", "/certs/mailpit.key")
                .withEnv("MP_SMTP_REQUIRE_TLS", "true");

        server.start();

        await().atMost(Duration.ofSeconds(10)).until(() -> server.isRunning()
                && server.getMappedPort(8025) > 0
                && server.getMappedPort(1025) > 0);
        await().atMost(Duration.ofSeconds(10)).catchUncaughtExceptions().untilAsserted(() -> {
            try {
                int status = RestAssured.get("http://" + server.getHost() + ":" + server.getMappedPort(8025) + "/api/v1/info")
                        .statusCode();
                Assertions.assertEquals(200, status);
            } catch (Exception e) {
                Assertions.fail();
            }
        });

        HashMap<String, String> properties = new HashMap<>();

        properties.put("mailpit-tls.port", Integer.toString(server.getMappedPort(1025)));
        properties.put("mailpit-tls.host", server.getHost());
        properties.put("mailpit-tls", server.getHost() + ":" + server.getMappedPort(8025));

        return properties;
    }

    @Override
    public void stop() {
        server.close();
    }
}
