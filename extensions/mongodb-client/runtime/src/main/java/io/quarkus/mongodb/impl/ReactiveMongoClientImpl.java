package io.quarkus.mongodb.impl;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.ListDatabasesPublisher;
import com.mongodb.reactivestreams.client.MongoClient;

import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.DatabaseListOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactiveMongoClientImpl implements ReactiveMongoClient {

    private final MongoClient client;

    public ReactiveMongoClientImpl(MongoClient client) {
        this.client = client;
    }

    @Override
    public ReactiveMongoDatabase getDatabase(String name) {
        return new ReactiveMongoDatabaseImpl(client.getDatabase(name));
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public Multi<String> listDatabaseNames() {
        return Wrappers.toMulti(client.listDatabaseNames());
    }

    @Override
    public Multi<String> listDatabaseNames(ClientSession clientSession) {
        return Wrappers.toMulti(client.listDatabaseNames(clientSession));
    }

    @Override
    public Multi<Document> listDatabases() {
        return Wrappers.toMulti(client.listDatabases());
    }

    @Override
    public Multi<Document> listDatabases(DatabaseListOptions options) {
        if (options != null) {
            ListDatabasesPublisher<Document> publisher = apply(options, client.listDatabases());
            return Wrappers.toMulti(publisher);
        } else {
            return listDatabases();
        }
    }

    private <T> ListDatabasesPublisher<T> apply(DatabaseListOptions options, ListDatabasesPublisher<T> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    private <T> ChangeStreamPublisher<T> apply(ChangeStreamOptions options, ChangeStreamPublisher<T> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    @Override
    public <T> Multi<T> listDatabases(Class<T> clazz) {
        return Wrappers.toMulti(client.listDatabases(clazz));
    }

    @Override
    public <T> Multi<T> listDatabases(Class<T> clazz, DatabaseListOptions options) {
        if (options != null) {
            ListDatabasesPublisher<T> publisher = apply(options, client.listDatabases(clazz));
            return Wrappers.toMulti(publisher);
        } else {
            return listDatabases(clazz);
        }
    }

    @Override
    public Multi<Document> listDatabases(ClientSession clientSession) {
        return Wrappers.toMulti(client.listDatabases(clientSession));
    }

    @Override
    public Multi<Document> listDatabases(ClientSession clientSession, DatabaseListOptions options) {
        ListDatabasesPublisher<Document> publisher = apply(options, client.listDatabases(clientSession));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public <T> Multi<T> listDatabases(ClientSession clientSession, Class<T> clazz) {
        return Wrappers.toMulti(client.listDatabases(clientSession, clazz));
    }

    @Override
    public <T> Multi<T> listDatabases(ClientSession clientSession, Class<T> clazz, DatabaseListOptions options) {
        return Wrappers.toMulti(apply(options, client.listDatabases(clientSession, clazz)));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch() {
        return Wrappers.toMulti(client.watch());
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options) {
        ChangeStreamPublisher<Document> publisher = apply(options, client.watch());
        return Wrappers.toMulti(publisher);
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz) {
        return Wrappers.toMulti(client.watch(clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options) {
        ChangeStreamPublisher<T> publisher = apply(options, client.watch(clazz));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline) {
        return Wrappers.toMulti(client.watch(pipeline));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options) {
        ChangeStreamPublisher<Document> publisher = apply(options, client.watch(pipeline));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz) {
        return Wrappers.toMulti(client.watch(pipeline, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options) {
        ChangeStreamPublisher<T> publisher = apply(options, client.watch(pipeline, clazz));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession) {
        return Wrappers.toMulti(client.watch(clientSession));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options) {
        ChangeStreamPublisher<Document> publisher = apply(options, client.watch(clientSession));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz) {
        return Wrappers.toMulti(client.watch(clientSession, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options) {
        ChangeStreamPublisher<T> publisher = apply(options, client.watch(clientSession, clazz));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession,
            List<? extends Bson> pipeline) {
        return Wrappers.toMulti(client.watch(clientSession, pipeline));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options) {
        ChangeStreamPublisher<Document> publisher = apply(options, client.watch(clientSession, pipeline));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz) {
        return Wrappers.toMulti(client.watch(clientSession, pipeline, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options) {
        ChangeStreamPublisher<T> publisher = apply(options, client.watch(clientSession, pipeline, clazz));
        return Wrappers.toMulti(publisher);
    }

    @Override
    public Uni<ClientSession> startSession() {
        return Wrappers.toUni(client.startSession());
    }

    @Override
    public Uni<ClientSession> startSession(ClientSessionOptions options) {
        return Wrappers.toUni(client.startSession(options));
    }

    @Override
    public MongoClient unwrap() {
        return client;
    }
}
