package io.quarkus.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongo.impl.ReactiveMongoClientImpl;

class ConnectionToReplicaSetTest extends MongoWithReplicasTestBase {

    private ReactiveMongoClient client;

    @AfterEach
    void cleanup() {
        client.close();
    }

    @Test
    void testConnection() {
        String cs = "mongodb://localhost:27018,localhost:27019";
        client = new ReactiveMongoClientImpl(MongoClients.create(cs));
        assertThat(client.listDatabases().findFirst().run().toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    void testConnectionWithReplicaSet() {
        String cs = "mongodb://localhost:27018,localhost:27019/?replicaSet=test001";
        client = new ReactiveMongoClientImpl(MongoClients.create(cs));
        assertThat(client.listDatabases().findFirst().run().toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    void testThatWatchStreamCanBeConnected() {
        String cs = "mongodb://localhost:27018,localhost:27019";
        client = new ReactiveMongoClientImpl(MongoClients.create(cs));
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        client.watch().onError(failures::add).ignore().run();
        client.watch(Document.class).onError(failures::add).ignore().run();
        client.watch(Collections.emptyList()).onError(failures::add).ignore().run();
        client.watch(Collections.emptyList(), Document.class).onError(failures::add).ignore().run();
        client.watch(Collections.emptyList(), Document.class, null).onError(failures::add).ignore().run();
        client.watch(Collections.emptyList(), Document.class,
                new ChangeStreamOptions().maxAwaitTime(1, TimeUnit.SECONDS)).onError(failures::add).ignore().run();
        client.watch(new ChangeStreamOptions().fullDocument(FullDocument.DEFAULT))
                .onError(failures::add).ignore().run();
        client.watch((ChangeStreamOptions) null).onError(failures::add).ignore().run();
        client.watch(Document.class, new ChangeStreamOptions().collation(Collation.builder().locale("simple").build()))
                .onError(failures::add).ignore().run();

        ReactiveStreams.fromPublisher(client.watchAsPublisher()).onError(failures::add).ignore().run();
        ReactiveStreams.fromPublisher(client.watchAsPublisher(Document.class)).onError(failures::add).ignore().run();
        ReactiveStreams.fromPublisher(client.watchAsPublisher(Collections.emptyList())).onError(failures::add).ignore().run();

        System.out.println("Failures are: " + failures);
        assertThat(failures).isEmpty();
    }

}
