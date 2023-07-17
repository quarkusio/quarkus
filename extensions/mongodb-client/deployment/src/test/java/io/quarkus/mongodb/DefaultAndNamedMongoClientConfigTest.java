package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.mongodb.health.MongoHealthCheck;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultAndNamedMongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("application-default-and-named-mongoclient.properties");

    @Inject
    MongoClient client;

    @Inject
    @MongoClientName("cluster2")
    MongoClient client2;

    @Inject
    @Any
    MongoHealthCheck health;

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
        if (client2 != null) {
            client2.close();
        }
    }

    @Test
    public void testNamedDataSourceInjection() {
        assertProperConnection(client, 27018);
        assertProperConnection(client2, 27019);

        assertThat(client.listDatabases().first()).isNotEmpty();
        assertThat(client2.listDatabases().first()).isNotEmpty();

        assertThat(Arc.container().instance(MongoClient.class).get()).isNotNull();
        assertThat(Arc.container().instance(MongoClient.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(MongoClient.class, NamedLiteral.of("cluster2")).get()).isNotNull();
        assertThat(Arc.container().instance(MongoClient.class, NamedLiteral.of("cluster3")).get()).isNull();

        org.eclipse.microprofile.health.HealthCheckResponse response = health.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get()).hasSize(2).contains(
                entry("<default>", "OK"),
                entry("cluster2", "OK"));

    }

    private void assertProperConnection(MongoClient client, int expectedPort) {
        assertThat(ClientProxy.unwrap(client)).isInstanceOfSatisfying(MongoClientImpl.class, c -> {
            assertThat(c.getCluster().getSettings().getHosts()).singleElement().satisfies(sa -> {
                assertThat(sa.getPort()).isEqualTo(expectedPort);
            });
        });
    }
}
