package io.quarkus.mongodb.impl;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.ListCollectionsPublisher;
import com.mongodb.reactivestreams.client.MongoDatabase;

import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.CollectionListOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactiveMongoDatabaseImpl implements ReactiveMongoDatabase {

    private final MongoDatabase database;

    ReactiveMongoDatabaseImpl(MongoDatabase database) {
        this.database = database;
    }

    @Override
    public String getName() {
        return database.getName();
    }

    @Override
    public ReactiveMongoCollection<Document> getCollection(String collectionName) {
        return new ReactiveMongoCollectionImpl<>(database.getCollection(collectionName));
    }

    @Override
    public <T> ReactiveMongoCollection<T> getCollection(String collectionName, Class<T> clazz) {
        return new ReactiveMongoCollectionImpl<>(database.getCollection(collectionName, clazz));
    }

    @Override
    public Uni<Document> runCommand(Bson command) {
        return Wrappers.toUni(database.runCommand(command));
    }

    @Override
    public Uni<Document> runCommand(Bson command, ReadPreference readPreference) {
        return Wrappers.toUni(database.runCommand(command, readPreference));
    }

    @Override
    public <T> Uni<T> runCommand(Bson command, Class<T> clazz) {
        return Wrappers.toUni(database.runCommand(command, clazz));
    }

    @Override
    public <T> Uni<T> runCommand(Bson command, ReadPreference readPreference, Class<T> clazz) {
        return Wrappers.toUni(database.runCommand(command, readPreference, clazz));
    }

    @Override
    public Uni<Document> runCommand(ClientSession clientSession, Bson command) {
        return Wrappers.toUni(database.runCommand(clientSession, command));
    }

    @Override
    public Uni<Document> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference) {
        return Wrappers.toUni(database.runCommand(clientSession, command, readPreference));
    }

    @Override
    public <T> Uni<T> runCommand(ClientSession clientSession, Bson command, Class<T> clazz) {
        return Wrappers.toUni(database.runCommand(clientSession, command, clazz));
    }

    @Override
    public <T> Uni<T> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference,
            Class<T> clazz) {
        return Wrappers.toUni(database.runCommand(clientSession, command, readPreference, clazz));
    }

    @Override
    public Uni<Void> drop() {
        return Wrappers.toUni(database.drop());
    }

    @Override
    public Uni<Void> drop(ClientSession clientSession) {
        return Wrappers.toUni(database.drop(clientSession));
    }

    @Override
    public Multi<String> listCollectionNames() {
        return Wrappers.toMulti(database.listCollectionNames());
    }

    @Override
    public Multi<String> listCollectionNames(ClientSession clientSession) {
        return Wrappers.toMulti(database.listCollectionNames(clientSession));
    }

    @Override
    public Multi<Document> listCollections() {
        return Wrappers.toMulti(database.listCollections());
    }

    @Override
    public Multi<Document> listCollections(CollectionListOptions options) {
        return Multi.createFrom().publisher(apply(options, database.listCollections()));
    }

    @Override
    public <T> Multi<T> listCollections(Class<T> clazz) {
        return Wrappers.toMulti(database.listCollections(clazz));
    }

    @Override
    public <T> Multi<T> listCollections(Class<T> clazz, CollectionListOptions options) {
        return Multi.createFrom().publisher(apply(options, database.listCollections(clazz)));
    }

    private <T> ListCollectionsPublisher<T> apply(CollectionListOptions options,
            ListCollectionsPublisher<T> collections) {
        if (options == null) {
            return collections;
        } else {
            return options.apply(collections);
        }
    }

    @Override
    public Multi<Document> listCollections(ClientSession clientSession) {
        return Wrappers.toMulti(database.listCollections(clientSession));
    }

    @Override
    public Multi<Document> listCollections(ClientSession clientSession, CollectionListOptions options) {
        return Multi.createFrom().publisher(apply(options, database.listCollections(clientSession)));
    }

    @Override
    public <T> Multi<T> listCollections(ClientSession clientSession, Class<T> clazz) {
        return Wrappers.toMulti(database.listCollections(clientSession, clazz));
    }

    @Override
    public <T> Multi<T> listCollections(ClientSession clientSession, Class<T> clazz, CollectionListOptions options) {
        return Multi.createFrom().publisher(apply(options, database.listCollections(clientSession, clazz)));
    }

    @Override
    public Uni<Void> createCollection(String collectionName) {
        return Wrappers.toUni(database.createCollection(collectionName));
    }

    @Override
    public Uni<Void> createCollection(String collectionName, CreateCollectionOptions options) {
        return Wrappers.toUni(database.createCollection(collectionName, options));
    }

    @Override
    public Uni<Void> createCollection(ClientSession clientSession, String collectionName) {
        return Wrappers.toUni(database.createCollection(clientSession, collectionName));
    }

    @Override
    public Uni<Void> createCollection(ClientSession clientSession, String collectionName,
            CreateCollectionOptions options) {
        return Wrappers.toUni(database.createCollection(clientSession, collectionName, options));
    }

    @Override
    public Uni<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline) {
        return Wrappers.toUni(database.createView(viewName, viewOn, pipeline));
    }

    @Override
    public Uni<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions) {
        return Wrappers.toUni(database.createView(viewName, viewOn, pipeline, createViewOptions));
    }

    @Override
    public Uni<Void> createView(ClientSession clientSession, String viewName, String viewOn,
            List<? extends Bson> pipeline) {
        return Wrappers.toUni(database.createView(clientSession, viewName, viewOn, pipeline));
    }

    @Override
    public Uni<Void> createView(ClientSession clientSession, String viewName, String viewOn,
            List<? extends Bson> pipeline, CreateViewOptions createViewOptions) {
        return Wrappers
                .toUni(database.createView(clientSession, viewName, viewOn, pipeline, createViewOptions));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch() {
        return Wrappers.toMulti(database.watch());
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz) {
        return Wrappers.toMulti(database.watch(clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline) {
        return Wrappers.toMulti(database.watch(pipeline));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz) {
        return Wrappers.toMulti(database.watch(pipeline, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession) {
        return Wrappers.toMulti(database.watch(clientSession));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz) {
        return Wrappers.toMulti(database.watch(clientSession, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toMulti(database.watch(clientSession, pipeline));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz) {
        return Wrappers.toMulti(database.watch(clientSession, pipeline, clazz));
    }

    @Override
    public <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public Multi<Document> aggregate(List<? extends Bson> pipeline) {
        return Wrappers.toMulti(database.aggregate(pipeline));
    }

    @Override
    public Multi<Document> aggregate(List<? extends Bson> pipeline, AggregateOptions options) {
        return Wrappers.toMulti(apply(options, database.aggregate(pipeline)));
    }

    private <T> AggregatePublisher<T> apply(AggregateOptions options, AggregatePublisher<T> aggregate) {
        if (options == null) {
            return aggregate;
        }
        return options.apply(aggregate);
    }

    @Override
    public <T> Multi<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz) {
        return Wrappers.toMulti(database.aggregate(pipeline, clazz));
    }

    @Override
    public <T> Multi<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz, AggregateOptions options) {
        return Wrappers.toMulti(apply(options, database.aggregate(pipeline, clazz)));
    }

    @Override
    public Multi<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toMulti(database.aggregate(clientSession, pipeline));
    }

    @Override
    public Multi<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            AggregateOptions options) {
        return Wrappers.toMulti(apply(options, database.aggregate(clientSession, pipeline)));
    }

    @Override
    public <T> Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz) {
        return Wrappers.toMulti(database.aggregate(clientSession, pipeline, clazz));
    }

    @Override
    public <T> Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz,
            AggregateOptions options) {
        return Wrappers.toMulti(apply(options, database.aggregate(clientSession, pipeline, clazz)));
    }

    @Override
    public MongoDatabase unwrap() {
        return database;
    }
}
