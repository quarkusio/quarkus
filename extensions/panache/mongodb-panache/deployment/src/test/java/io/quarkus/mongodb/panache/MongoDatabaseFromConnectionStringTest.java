package io.quarkus.mongodb.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.test.QuarkusExtensionTest;

class MongoDatabaseFromConnectionStringTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DefaultEntity.class, DefaultReactiveEntity.class, NamedEntity.class,
                            NamedReactiveEntity.class, ConfiguredEntity.class, ConfiguredReactiveEntity.class)
                    .addAsResource(new StringAsset("""
                            quarkus.mongodb.devservices.enabled=false
                            quarkus.mongodb.connection-string=mongodb://localhost:27018/default-uri-db
                            quarkus.mongodb.named.connection-string=mongodb://localhost:27018/named-uri-db
                            quarkus.mongodb.configured.connection-string=mongodb://localhost:27018/ignored-uri-db
                            quarkus.mongodb.configured.database=configured-db
                            """), "application.properties"));

    @Test
    void usesDatabaseFromDefaultClientConnectionString() {
        assertEquals("default-uri-db", DefaultEntity.mongoDatabase().getName());
        assertEquals("default-uri-db", DefaultReactiveEntity.mongoDatabase().getName());
    }

    @Test
    void usesDatabaseFromNamedClientConnectionString() {
        assertEquals("named-uri-db", NamedEntity.mongoDatabase().getName());
        assertEquals("named-uri-db", NamedReactiveEntity.mongoDatabase().getName());
    }

    @Test
    void explicitDatabaseTakesPriorityOverConnectionString() {
        assertEquals("configured-db", ConfiguredEntity.mongoDatabase().getName());
        assertEquals("configured-db", ConfiguredReactiveEntity.mongoDatabase().getName());
    }

    static class DefaultEntity extends PanacheMongoEntity {
    }

    static class DefaultReactiveEntity extends ReactivePanacheMongoEntity {
    }

    @MongoEntity(clientName = "named")
    static class NamedEntity extends PanacheMongoEntity {
    }

    @MongoEntity(clientName = "named")
    static class NamedReactiveEntity extends ReactivePanacheMongoEntity {
    }

    @MongoEntity(clientName = "configured")
    static class ConfiguredEntity extends PanacheMongoEntity {
    }

    @MongoEntity(clientName = "configured")
    static class ConfiguredReactiveEntity extends ReactivePanacheMongoEntity {
    }
}
