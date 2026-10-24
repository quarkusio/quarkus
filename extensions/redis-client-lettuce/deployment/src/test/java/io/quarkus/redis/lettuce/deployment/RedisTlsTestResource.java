package io.quarkus.redis.lettuce.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

/**
 * Generates a self-signed server certificate (CN and SAN {@code localhost} only, so that {@code 127.0.0.1} is a
 * host name mismatch) plus a client certificate into
 * {@link #CERTS_DIR} under the name {@link #CERT_NAME}, and starts two TLS-only Redis servers with it:
 * <ul>
 * <li>{@code redis.tls.host} / {@code redis.tls.port}: server authentication only;</li>
 * <li>{@code redis.mtls.host} / {@code redis.mtls.port}: client certificates required (mutual TLS) and a password
 * ({@code redis.mtls.password}).</li>
 * </ul>
 * The generated PEM files are {@code redis.crt} / {@code redis.key} (server), {@code redis-client-ca.crt} (what the
 * client trusts), {@code redis-client.crt} / {@code redis-client.key} (client) and {@code redis-server-ca.crt}
 * (what the server trusts).
 * <p>
 * This resource must stay global (no {@code restrictToAnnotatedClass}). {@code QuarkusExtensionTest} creates a single
 * {@code TestResourceManager} per JVM, in the JUnit root store, from the first test class that runs: global
 * resources found in the test class index are all started at that point, whereas a restricted resource is only
 * started when that first class declares it. A restricted TLS resource would therefore never start when, say,
 * {@code LettuceInjectionTest} runs first, and every TLS test would fail. Being started once for the whole module,
 * before the first test class, is also why the certificates are generated here rather than with the JUnit
 * {@code @Certificates} extension of a particular test class.
 */
public class RedisTlsTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String CERTS_DIR = "target/certs";
    public static final String CERT_NAME = "redis";
    public static final String MTLS_PASSWORD = "s3cr3t-tls";

    private GenericContainer<?> tls;
    private GenericContainer<?> mtls;

    @Override
    public Map<String, String> start() {
        generateCertificates();

        tls = server(false, null);
        mtls = server(true, MTLS_PASSWORD);
        tls.start();
        mtls.start();

        Map<String, String> properties = new HashMap<>();
        properties.put("redis.tls.host", tls.getHost());
        properties.put("redis.tls.port", String.valueOf(tls.getMappedPort(6379)));
        properties.put("redis.mtls.host", mtls.getHost());
        properties.put("redis.mtls.port", String.valueOf(mtls.getMappedPort(6379)));
        properties.put("redis.mtls.password", MTLS_PASSWORD);
        return properties;
    }

    private static void generateCertificates() {
        try {
            // the generator writes into the directory but does not create it
            Files.createDirectories(Path.of(CERTS_DIR));
            new CertificateGenerator(Path.of(CERTS_DIR), true).generate(new CertificateRequest()
                    .withName(CERT_NAME)
                    .withFormat(Format.PEM)
                    .withClientCertificate()
                    .withCN("localhost")
                    .withSubjectAlternativeName("DNS:localhost"));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate the Redis TLS certificates", e);
        }
    }

    private static GenericContainer<?> server(boolean authClients, String password) {
        List<String> command = new ArrayList<>(List.of("redis-server",
                "--port", "0",
                "--tls-port", "6379",
                "--tls-cert-file", "/certs/server.crt",
                "--tls-key-file", "/certs/server.key",
                "--tls-ca-cert-file", "/certs/clients-ca.crt",
                "--tls-auth-clients", authClients ? "yes" : "no"));
        if (password != null) {
            command.addAll(List.of("--requirepass", password));
        }
        return new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
                .withCopyFileToContainer(cert(CERT_NAME + ".crt"), "/certs/server.crt")
                .withCopyFileToContainer(cert(CERT_NAME + ".key"), "/certs/server.key")
                .withCopyFileToContainer(cert(CERT_NAME + "-server-ca.crt"), "/certs/clients-ca.crt")
                .withCommand(command.toArray(String[]::new))
                .withExposedPorts(6379);
    }

    private static MountableFile cert(String fileName) {
        // world-readable so that the redis user inside the container can read the key
        return MountableFile.forHostPath(Path.of(CERTS_DIR, fileName), 0644);
    }

    @Override
    public void stop() {
        if (mtls != null) {
            mtls.stop();
        }
        if (tls != null) {
            tls.stop();
        }
    }
}
