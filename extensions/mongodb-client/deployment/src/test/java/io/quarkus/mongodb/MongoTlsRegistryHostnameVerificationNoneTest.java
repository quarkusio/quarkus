package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verifies that the MongoDB client honors {@code quarkus.tls.<name>.hostname-verification-algorithm=NONE} from the
 * TLS registry.
 * <p>
 * The server certificate is issued for {@code mongo.example.com} while the client connects to {@code 127.0.0.1},
 * so the connection only succeeds when hostname verification is disabled.
 */
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Tests don't pass on windows CI")
@Certificates(baseDir = MongoTlsRegistryHostnameVerificationNoneTest.BASEDIR, certificates = {
        @Certificate(name = "mongo-hostname-cert", formats = Format.PEM, client = true, cn = "mongo.example.com", subjectAlternativeNames = "DNS:mongo.example.com")
})
public class MongoTlsRegistryHostnameVerificationNoneTest extends MongoTestBase {
    static final String BASEDIR = "target/certs";
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("tls-mongoclient-hostname-none.properties");
    private final Path serverCertPath = Path.of(BASEDIR, "mongo-hostname-cert.crt");
    private final Path serverKeyPath = Path.of(BASEDIR, "mongo-hostname-cert.key");
    private final Path serverCaPath = Path.of(BASEDIR, "mongo-hostname-cert-server-ca.crt");
    private final Path serverCertKeyPath = Path.of(BASEDIR, "mongo-hostname-certkey.pem");
    @Inject
    MongoClient client;
    @Inject
    ReactiveMongoClient reactiveClient;

    @AfterEach
    void cleanup() {
        if (reactiveClient != null) {
            reactiveClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Override
    protected ImmutableMongod addExtraConfig(ImmutableMongod mongo) {
        try (var fos = Files.newOutputStream(serverCertKeyPath)) {
            Files.copy(serverCertPath, fos);
            Files.copy(serverKeyPath, fos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mongo.withMongodArguments(Start.to(mongo.mongodArguments().destination())
                .initializedWith(MongodArguments.builder()
                        .putArgs("--tlsCertificateKeyFile", serverCertKeyPath.toAbsolutePath().toString())
                        .putArgs("--tlsMode", "requireTLS")
                        .putArgs("--tlsCAFile", serverCaPath.toAbsolutePath().toString())
                        .build()));

    }

    @Test
    public void testClientWorksWithTlsAndHostnameVerificationDisabled() {
        assertThat(client.listDatabaseNames().first()).isNotEmpty();
        assertThat(reactiveClient.listDatabases().collect().first().await().indefinitely()).isNotEmpty();
    }
}
