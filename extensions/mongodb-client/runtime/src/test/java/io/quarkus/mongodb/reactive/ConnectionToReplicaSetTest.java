package io.quarkus.mongodb.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

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
        assertThat(client.listDatabases().collect().first().await().asOptional().indefinitely()).isNotEmpty();
    }

    @Test
    void testConnectionWithReplicaSet() {
        String cs = "mongodb://localhost:27018,localhost:27019/?replicaSet=test001";
        client = new ReactiveMongoClientImpl(MongoClients.create(cs));
        assertThat(client.listDatabases().collect().first().await().asOptional().indefinitely()).isNotEmpty();
    }

    @Test
    void testThatWatchStreamCanBeConnected() {
        String cs = "mongodb://localhost:27018,localhost:27019";
        client = new ReactiveMongoClientImpl(MongoClients.create(cs));
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        client.watch().onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Document.class).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Collections.emptyList()).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Collections.emptyList(), Document.class).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Collections.emptyList(), Document.class, null).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Collections.emptyList(), Document.class,
                new ChangeStreamOptions().maxAwaitTime(1, TimeUnit.SECONDS)).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(new ChangeStreamOptions().fullDocument(FullDocument.DEFAULT))
                .onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch((ChangeStreamOptions) null).onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        client.watch(Document.class)
                .onFailure().invoke(failures::add)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(failures).isEmpty();
    }

}
