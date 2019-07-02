package io.quarkus.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongo.impl.ReactiveMongoClientImpl;

public class DatabaseListTest extends MongoTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        client.getDatabase(DATABASE).drop().toCompletableFuture().join();
        client.close();
    }

    @Test
    void list() {
        assertThat(client.listDatabaseNames().toList().run().toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(ReactiveStreams.fromPublisher(client.listDatabaseNamesAsPublisher())
                .toList().run().toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(client.listDatabases().toList().run().toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(client.listDatabases(Document.class).toList().run().toCompletableFuture().join().size())
                .isBetween(2, 3);
        assertThat(client.listDatabases(Document.class, new DatabaseListOptions().maxTime(1, TimeUnit.SECONDS))
                .toList().run().toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(client.listDatabases(Document.class, null)
                .toList().run().toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(client.listDatabases(new DatabaseListOptions().nameOnly(true)).toList().run()
                .toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(client.listDatabases((DatabaseListOptions) null).toList().run().toCompletableFuture().join().size())
                .isBetween(2, 3);
        assertThat(ReactiveStreams.fromPublisher(client.listDatabasesAsPublisher()).toList().run()
                .toCompletableFuture().join().size()).isBetween(2, 3);
        assertThat(ReactiveStreams.fromPublisher(client.listDatabasesAsPublisher(Document.class)).toList().run()
                .toCompletableFuture().join().size()).isBetween(2, 3);
    }
}
