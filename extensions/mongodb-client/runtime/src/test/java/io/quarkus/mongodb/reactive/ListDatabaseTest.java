package io.quarkus.mongodb.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.bson.Document;
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
        List<Document> documents = client.listDatabases().collect().asList().await().indefinitely();
        assertThat(documents).hasSize(2);
        assertThat(documents).hasSize(2);
        assertThat(client.listDatabaseNames().collect().asList().await().indefinitely())
                .containsExactlyInAnyOrder("local", "admin");

        List<String> names = client.startSession()
                .chain(session -> client.listDatabaseNames(session).collect().asList())
                .await().indefinitely();
        assertThat(names).containsExactlyInAnyOrder("local", "admin");

        names = client.startSession()
                .chain(session -> client.listDatabases(session).map(doc -> doc.getString("name")).collect().asList())
                .await().indefinitely();
        assertThat(names).containsExactlyInAnyOrder("local", "admin");
    }

    @Test
    void testSessionCreation() {
        // Session requires replicas
        ClientSession session = client.startSession(ClientSessionOptions.builder().causallyConsistent(true).build())
                .await().indefinitely();
        assertThat(session).isNotNull();
        session.close();
    }
}
