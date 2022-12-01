package io.quarkus.mongodb.panache.bug10812;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class Bug10812MongodbMultiClientsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            Bug10812BookNotAnnotatedEntity.class,
                            Bug10812BookDefaultClientNameEntity.class,
                            Bug10812BookClient1Entity.class,
                            Bug10812BookClient2Entity.class,
                            Bug10812BookNotAnnotatedReactiveEntity.class,
                            Bug10812BookDefaultClientNameReactiveEntity.class,
                            Bug10812BookClient1ReactiveEntity.class,
                            Bug10812BookClient2ReactiveEntity.class)
                    .addAsResource("application.properties"));

    @Test
    public void testMongoDatabaseNameConfigurationWhenEntityIsNotAnnotated() {
        assertEquals("test", Bug10812BookNotAnnotatedEntity.mongoDatabase().getName());
        assertEquals("test", Bug10812BookNotAnnotatedReactiveEntity.mongoDatabase().getName());
    }

    @Test
    public void testMongoDatabaseNameConfigurationWhenEntityUsesDefaultClientName() {
        assertEquals("test", Bug10812BookDefaultClientNameEntity.mongoDatabase().getName());
        assertEquals("test", Bug10812BookDefaultClientNameReactiveEntity.mongoDatabase().getName());
    }

    @Test
    public void testMongoDatabaseNameConfigurationWhenEntityHasClientNameAndUseSpecificDatabaseName() {
        assertEquals("cl1-10812-db", Bug10812BookClient1Entity.mongoDatabase().getName());
        assertEquals("cl1-10812-db", Bug10812BookClient1ReactiveEntity.mongoDatabase().getName());
    }

    @Test
    public void testMongoDatabaseNameConfigurationWhenEntityHasClientNameAndUseDefaultDatabaseName() {
        assertEquals("test", Bug10812BookClient2Entity.mongoDatabase().getName());
        assertEquals("test", Bug10812BookClient2ReactiveEntity.mongoDatabase().getName());
    }
}
