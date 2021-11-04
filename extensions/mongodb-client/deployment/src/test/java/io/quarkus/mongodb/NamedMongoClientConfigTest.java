package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.health.MongoHealthCheck;
import io.quarkus.test.QuarkusUnitTest;

public class NamedMongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("application-named-mongoclient.properties");

    @Inject
    @MongoClientName("cluster1")
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
        assertThat(client.listDatabases().first()).isNotEmpty();
        assertThat(client2.listDatabases().first()).isNotEmpty();

        assertNoDefaultClient();

        checkHealth();
    }

    public void checkHealth() {
        org.eclipse.microprofile.health.HealthCheckResponse response = health.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get()).hasSize(2).contains(entry("cluster1", "OK"), entry("cluster2", "OK"));
    }

    private void assertNoDefaultClient() {
        boolean hasDefault = false;
        for (InstanceHandle<MongoClient> handle : Arc.container().select(MongoClient.class).handles()) {
            InjectableBean<MongoClient> bean = handle.getBean();
            for (Annotation qualifier : bean.getQualifiers()) {
                if (qualifier.annotationType().equals(Default.class)) {
                    hasDefault = true;
                }
            }
        }
        Assertions.assertFalse(hasDefault,
                "The default mongo client should not have been present as it is not used in any injection point");
    }
}
