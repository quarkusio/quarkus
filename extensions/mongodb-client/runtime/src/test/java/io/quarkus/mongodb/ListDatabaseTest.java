package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.bson.Document;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ClientSessionOptions;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

class ListDatabaseTest extends MongoWithReplicasTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        client.close();
    }

    @Test
    void testListingDatabasesWithNoCreatedDatabases() {
        // local, and admin are created by default in replicas mode (admin because of the replicas registration)
        List<Document> documents = client.listDatabases().toList().run().toCompletableFuture().join();
        assertThat(documents).hasSize(2);
        documents = ReactiveStreams.fromPublisher(client.listDatabasesAsPublisher()).toList().run().toCompletableFuture()
                .join();
        assertThat(documents).hasSize(2);
        assertThat(client.listDatabaseNames().toList().run().toCompletableFuture().join())
                .containsExactlyInAnyOrder("local", "admin");
        assertThat(ReactiveStreams.fromPublisher(client.listDatabaseNamesAsPublisher()).toList().run().toCompletableFuture()
                .join())
                        .containsExactlyInAnyOrder("local", "admin");

        List<String> names = client.startSession()
                .thenCompose(session -> client.listDatabaseNames(session).toList().run())
                .toCompletableFuture()
                .join();
        assertThat(names).containsExactlyInAnyOrder("local", "admin");

        names = client.startSession()
                .thenCompose(session -> client.listDatabases(session).map(doc -> doc.getString("name")).toList().run())
                .toCompletableFuture()
                .join();
        assertThat(names).containsExactlyInAnyOrder("local", "admin");

    }

    @Test
    void testSessionCreation() {
        // Session requires replicas
        ClientSession session = client.startSession(ClientSessionOptions.builder().causallyConsistent(true).build())
                .toCompletableFuture().join();
        assertThat(session).isNotNull();
        session.close();
    }
}
