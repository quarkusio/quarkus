package io.quarkus.mongodb;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;

import io.quarkus.arc.ClientProxy;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Flapdoodle doesn't work very well on Windows with replicas")
public class MongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("test-config-mongoclient.properties");

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

    @Test
    public void testClientConfiguration() {
        MongoClientImpl clientImpl = (MongoClientImpl) ClientProxy.unwrap(client);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxSize()).isEqualTo(2);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMinSize()).isEqualTo(1);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.SECONDS))
                .isEqualTo(5);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.SECONDS))
                .isEqualTo(60);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaintenanceFrequency(TimeUnit.SECONDS))
                .isEqualTo(60);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaintenanceInitialDelay(TimeUnit.SECONDS))
                .isEqualTo(5);
        assertThat(clientImpl.getSettings().getSocketSettings().getConnectTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getSocketSettings().getReadTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getClusterSettings().getServerSelectionTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getClusterSettings().getLocalThreshold(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getServerSettings().getHeartbeatFrequency(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getServerSettings().getHeartbeatFrequency(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getWriteConcern().getW()).isEqualTo(1);
        assertThat(clientImpl.getSettings().getWriteConcern().getWTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getReadConcern()).isEqualTo(new ReadConcern(ReadConcernLevel.SNAPSHOT));
        assertThat(clientImpl.getSettings().getReadPreference()).isEqualTo(ReadPreference.primary());
    }

    @Test
    public void testReactiveClientConfiuration() {
        ReactiveMongoClientImpl reactiveMongoClientImpl = (ReactiveMongoClientImpl) ClientProxy.unwrap(reactiveClient);
        com.mongodb.reactivestreams.client.internal.MongoClientImpl clientImpl = (com.mongodb.reactivestreams.client.internal.MongoClientImpl) reactiveMongoClientImpl
                .unwrap();
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxSize()).isEqualTo(2);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMinSize()).isEqualTo(1);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.SECONDS))
                .isEqualTo(5);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.SECONDS))
                .isEqualTo(60);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaintenanceFrequency(TimeUnit.SECONDS))
                .isEqualTo(60);
        assertThat(clientImpl.getSettings().getConnectionPoolSettings().getMaintenanceInitialDelay(TimeUnit.SECONDS))
                .isEqualTo(5);
        assertThat(clientImpl.getSettings().getSocketSettings().getConnectTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getSocketSettings().getReadTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getClusterSettings().getServerSelectionTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getClusterSettings().getLocalThreshold(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getServerSettings().getHeartbeatFrequency(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getWriteConcern().getW()).isEqualTo(1);
        assertThat(clientImpl.getSettings().getWriteConcern().getWTimeout(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(clientImpl.getSettings().getReadConcern()).isEqualTo(new ReadConcern(ReadConcernLevel.SNAPSHOT));
        assertThat(clientImpl.getSettings().getReadPreference()).isEqualTo(ReadPreference.primary());
    }

    @Test
    public void healthCheck() {
        when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));
    }
}
