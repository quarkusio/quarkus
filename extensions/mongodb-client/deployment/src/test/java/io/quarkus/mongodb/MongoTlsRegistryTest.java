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
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Tests don't pass on windows CI")
@Certificates(baseDir = MongoTlsRegistryTest.BASEDIR, certificates = {
        @Certificate(name = "mongo-cert", formats = Format.PEM, client = true)
})
public class MongoTlsRegistryTest extends MongoTestBase {
    static final String BASEDIR = "target/certs";
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("tls-mongoclient.properties");
    private static final Path BASEPATH = Path.of(BASEDIR);
    private final Path serverCertPath = Path.of(BASEDIR, "mongo-cert.crt");
    private final Path serverKeyPath = Path.of(BASEDIR, "mongo-cert.key");
    private final Path serverCaPath = Path.of(BASEDIR, "mongo-cert-server-ca.crt");
    private final Path serverCertKeyPath = Path.of(BASEDIR, "mongo-certkey.pem");
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
    public void testClientWorksWithTls() {
        assertThat(client.listDatabaseNames().first()).isNotEmpty();
        assertThat(reactiveClient.listDatabases().collect().first().await().indefinitely()).isNotEmpty();
    }
}
