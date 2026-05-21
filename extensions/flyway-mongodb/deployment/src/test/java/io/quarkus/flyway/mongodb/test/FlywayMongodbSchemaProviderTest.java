package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ServiceLoader;

import jakarta.inject.Inject;

import org.bson.Document;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbSchemaProvider;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbSchemaProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.js",
                            "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "schemaprov")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "schemaprov")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void serviceLoaderResolvesProvider() {
        ServiceLoader<DatabaseSchemaProvider> providers = ServiceLoader.load(DatabaseSchemaProvider.class);
        assertThat(providers)
                .extracting(p -> p.getClass().getName())
                .contains(FlywayMongodbSchemaProvider.class.getName());
    }

    @Test
    void resetDatabaseRunsCleanAndMigrate() {
        MongoCollection<Document> users = mongoClient.getDatabase("schemaprov").getCollection("users");
        users.insertOne(new Document("name", "extra"));
        assertThat(users.countDocuments()).isEqualTo(2L);

        DatabaseSchemaProvider provider = new FlywayMongodbSchemaProvider();
        provider.resetDatabase(MongoConfig.DEFAULT_CLIENT_NAME);

        assertThat(users.countDocuments()).isEqualTo(1L);
        assertThat(users.find().first().getString("name")).isEqualTo("alice");
    }

    @Test
    void resetAllDatabasesIsCallable() {
        DatabaseSchemaProvider provider = new FlywayMongodbSchemaProvider();
        provider.resetAllDatabases();

        MongoCollection<Document> users = mongoClient.getDatabase("schemaprov").getCollection("users");
        assertThat(users.countDocuments()).isEqualTo(1L);
    }
}
