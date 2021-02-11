package io.quarkus.mongodb.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.DatabaseListOptions;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

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
        Assertions.assertThat(client.listDatabases(Document.class, new DatabaseListOptions().maxTime(1, TimeUnit.SECONDS))
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
