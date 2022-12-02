package io.quarkus.mongodb.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ReadPreference;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

class DatabaseRunCommandTest extends MongoTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        client.getDatabase(DATABASE).drop().await().indefinitely();
        client.close();
    }

    @Test
    void run() {
        Document info = client.getDatabase(DATABASE).runCommand(new Document("buildInfo", 1)).await().indefinitely();
        assertThat(info.getDouble("ok")).isEqualTo(1.0);

        info = client.getDatabase(DATABASE).runCommand(new Document("buildInfo", 1), Document.class)
                .await().indefinitely();
        assertThat(info.getDouble("ok")).isEqualTo(1.0);

        info = client.getDatabase(DATABASE).runCommand(new Document("buildInfo", 1), ReadPreference.nearest())
                .await().indefinitely();
        assertThat(info.getDouble("ok")).isEqualTo(1.0);

        info = client.getDatabase(DATABASE).runCommand(new Document("buildInfo", 1), ReadPreference.nearest(), Document.class)
                .await().indefinitely();
        assertThat(info.getDouble("ok")).isEqualTo(1.0);
    }
}
