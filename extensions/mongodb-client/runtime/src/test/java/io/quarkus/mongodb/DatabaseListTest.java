package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.mutiny.ReactiveMongoClient;

public class DatabaseListTest extends MongoTestBase {

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
    void list() {
        assertThat(client.listDatabaseNames().collectItems().asList().await().indefinitely().size()).isBetween(2, 3);
        assertThat(client.listDatabases().collectItems().asList().await().indefinitely().size()).isBetween(2, 3);
        assertThat(client.listDatabases(Document.class).collectItems().asList().await().indefinitely().size())
                .isBetween(2, 3);
        assertThat(client.listDatabases(Document.class, new DatabaseListOptions().maxTime(1, TimeUnit.SECONDS))
                .collectItems().asList().await().indefinitely().size()).isBetween(2, 3);
        assertThat(client.listDatabases(Document.class, null)
                .collectItems().asList().await().indefinitely().size()).isBetween(2, 3);
        assertThat(client.listDatabases(new DatabaseListOptions().nameOnly(true)).collectItems().asList()
                .await().indefinitely().size()).isBetween(2, 3);
        assertThat(
                client.listDatabases((DatabaseListOptions) null).collectItems().asList().await().indefinitely().size())
                        .isBetween(2, 3);
    }
}
