package io.quarkus.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Flapdoodle doesn't work very well on Windows with replicas")
public class MongoActiveClientsTest extends MongoWithReplicasTestBase {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class));

    @Inject
    InjectableInstance<MongoClient> mongoClient;
    @Inject
    InjectableInstance<ReactiveMongoClient> reactiveMongoClient;

    @Inject
    @MongoClientName("active")
    InjectableInstance<MongoClient> activeMongoClient;
    @Inject
    @MongoClientName("active")
    InjectableInstance<ReactiveMongoClient> activeReactiveMongoClient;

    @Inject
    @Any
    InjectableInstance<MongoClient> all;
    @Inject
    @Any
    InjectableInstance<ReactiveMongoClient> allReactive;

    @Test
    void inactiveClients() {
        assertEquals(1, mongoClient.listActive().size());
        assertEquals(1, reactiveMongoClient.listActive().size());

        assertEquals(1, activeMongoClient.listActive().size());
        assertEquals(1, activeReactiveMongoClient.listActive().size());

        assertEquals(2, all.listActive().size());
        assertEquals(2, allReactive.listActive().size());
    }
}
