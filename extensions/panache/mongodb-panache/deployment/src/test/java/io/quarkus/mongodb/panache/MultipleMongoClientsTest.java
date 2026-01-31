package io.quarkus.mongodb.panache;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleMongoClientsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.mongodb.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addAsResource("named-clients.properties"));

    @Inject
    WithDefaultMongoClientEntityRepository withDefaultMongoClientEntityRepository;

    @Inject
    WithMongoClient1EntityRepository withMongoClient1EntityRepository;

    @Inject
    WithMongoClient2EntityRepository withMongoClient2EntityRepository;

    @Inject
    @MongoClientName("mongoclient1")
    MongoClient mongoClient1;

    @Inject
    @MongoClientName("mongoclient1")
    ReactiveMongoClient reactiveMongoClient1;

    @Inject
    @MongoClientName("mongoclient2")
    MongoClient mongoClient2;

    @Inject
    @MongoClientName("mongoclient2")
    ReactiveMongoClient reactiveMongoClient2;

    @Test
    public void testMongoEntity() {
        assertAll(
                () -> assertEquals("default-mongoclient-db", withDefaultMongoClientEntityRepository.mongoDatabase().getName()),
                () -> assertEquals("mongoclient1-db", withMongoClient1EntityRepository.mongoDatabase().getName()),
                () -> assertEquals("mongoclient2-db", withMongoClient2EntityRepository.mongoDatabase().getName()),
                () -> assertNotNull(reactiveMongoClient1),
                () -> assertNotNull(reactiveMongoClient2));
    }

    @MongoEntity(database = "default-mongoclient-db")
    public static class WithDefaultMongoClientEntity extends PanacheMongoEntity {
        public String field;
    }

    @MongoEntity(database = "mongoclient1-db", clientName = "mongoclient1")
    public static class WithMongoClient1Entity extends PanacheMongoEntity {
        public String field;
    }

    @MongoEntity(database = "mongoclient2-db", clientName = "mongoclient2")
    public static class WithMongoClient2Entity extends PanacheMongoEntity {
        public String field;
    }

    @ApplicationScoped
    public static class WithDefaultMongoClientEntityRepository implements PanacheMongoRepository<WithDefaultMongoClientEntity> {
    }

    @ApplicationScoped
    public static class WithMongoClient1EntityRepository implements PanacheMongoRepository<WithMongoClient1Entity> {
    }

    @ApplicationScoped
    public static class WithMongoClient2EntityRepository implements PanacheMongoRepository<WithMongoClient2Entity> {
    }

}
