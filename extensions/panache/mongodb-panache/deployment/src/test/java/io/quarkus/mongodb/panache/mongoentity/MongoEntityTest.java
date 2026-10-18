package io.quarkus.mongodb.panache.mongoentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MongoEntityTest {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            MongoEntityEntity.class,
                            MongoEntityRepository.class)
                    .addAsResource("application.properties"));

    @Inject
    MongoEntityRepository mongoEntityRepository;

    @Test
    public void testMongoEntity() {
        assertEquals("mongoEntity", MongoEntityEntity.mongoDatabase().getName());
        assertEquals("mongoEntity", mongoEntityRepository.mongoDatabase().getName());
    }

}
