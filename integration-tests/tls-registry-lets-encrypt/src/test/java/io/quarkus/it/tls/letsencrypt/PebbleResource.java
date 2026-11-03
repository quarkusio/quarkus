package io.quarkus.it.tls.letsencrypt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import io.quarkus.test.common.DockerImageNames;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts Pebble, the test ACME server of Let's Encrypt, on the host network, so that it validates the HTTP-01
 * challenge on the HTTP port of the tests.
 */
public class PebbleResource implements QuarkusTestResourceLifecycleManager {

    static final int ACME_PORT = 14000;
    static final int MANAGEMENT_PORT = 15000;
    static final int HTTP_PORT = 8081;
    static final Path STORAGE = Path.of("target/lets-encrypt");
    static final Path PEBBLE_CA = Path.of("target/pebble-minica.pem");

    private static final String CONFIG = """
            {
              "pebble": {
                "listenAddress": "0.0.0.0:%d",
                "managementListenAddress": "0.0.0.0:%d",
                "certificate": "test/certs/localhost/cert.pem",
                "privateKey": "test/certs/localhost/key.pem",
                "httpPort": %d,
                "tlsPort": 5001,
                "ocspResponderURL": "",
                "externalAccountBindingRequired": false,
                "retryAfter": { "authz": 1, "order": 1 },
                "keyAlgorithm": "ecdsa",
                "profiles": {
                  "default": { "description": "default", "validityPeriod": 7776000 }
                }
              }
            }
            """.formatted(ACME_PORT, MANAGEMENT_PORT, HTTP_PORT);

    private GenericContainer<?> pebble;

    @Override
    public Map<String, String> start() {
        deleteStorage();
        pebble = new GenericContainer<>(DockerImageNames.getImage("pebble.image"))
                .withNetworkMode("host")
                .withCopyToContainer(Transferable.of(CONFIG), "/test/config/quarkus-pebble.json")
                .withCommand("-config", "/test/config/quarkus-pebble.json")
                .withEnv("PEBBLE_VA_NOSLEEP", "1")
                // the client retries a rejected nonce once; two rejections in a row would make the test flaky
                .withEnv("PEBBLE_WFE_NONCEREJECT", "0")
                .waitingFor(Wait.forLogMessage(".*Listening on: 0.0.0.0:" + ACME_PORT + ".*", 1));
        pebble.start();
        pebble.copyFileFromContainer("/test/certs/pebble.minica.pem", PEBBLE_CA.toString());
        return Map.of(
                "quarkus.http.test-port", Integer.toString(HTTP_PORT),
                "quarkus.tls.lets-encrypt.client.directory-url", "https://localhost:" + ACME_PORT + "/dir",
                "quarkus.tls.pebble.trust-store.pem.certs", PEBBLE_CA.toAbsolutePath().toString());
    }

    @Override
    public void stop() {
        if (pebble != null) {
            pebble.stop();
        }
    }

    private static void deleteStorage() {
        if (!Files.exists(STORAGE)) {
            return;
        }
        try (Stream<Path> files = Files.walk(STORAGE)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
