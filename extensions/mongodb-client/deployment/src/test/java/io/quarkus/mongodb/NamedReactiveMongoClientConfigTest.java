package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientName;
import io.quarkus.test.QuarkusUnitTest;

public class NamedReactiveMongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("application-named-mongoclient.properties");

    @Inject
    @MongoClientName("cluster1")
    ReactiveMongoClient client;

    @Inject
    @MongoClientName("cluster2")
    ReactiveMongoClient client2;

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
        assertThat(client.listDatabases().collectItems().first().await().indefinitely()).isNotEmpty();
        assertThat(client2.listDatabases().collectItems().first().await().indefinitely()).isNotEmpty();

        assertNoDefaultClient();
    }

    private void assertNoDefaultClient() {
        boolean hasDefault = false;
        for (InstanceHandle<ReactiveMongoClient> handle : Arc.container().select(ReactiveMongoClient.class).handles()) {
            InjectableBean<ReactiveMongoClient> bean = handle.getBean();
            for (Annotation qualifier : bean.getQualifiers()) {
                if (qualifier.annotationType().equals(Default.class)) {
                    hasDefault = true;
                }
            }
        }
        Assertions.assertFalse(hasDefault,
                "The default reactive mongo client should not have been present as it is not used in any injection point");
    }
}
