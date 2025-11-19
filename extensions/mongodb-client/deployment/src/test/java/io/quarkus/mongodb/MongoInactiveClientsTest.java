package io.quarkus.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class MongoInactiveClientsTest extends MongoWithReplicasTestBase {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .overrideRuntimeConfigKey("quarkus.mongodb.inactive.hosts", "")
            .overrideRuntimeConfigKey("quarkus.mongodb.inactive.connection-string", "");

    @Inject
    @MongoClientName("inactive")
    InjectableInstance<MongoClient> mongoClient;
    @Inject
    @MongoClientName("inactive")
    InjectableInstance<ReactiveMongoClient> reactiveMongoClient;

    @Test
    void inactiveClients() {
        assertEquals(0, mongoClient.listActive().size());
        assertEquals(0, reactiveMongoClient.listActive().size());
    }
}
