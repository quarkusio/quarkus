package io.quarkus.mongo.impl;

import static io.quarkus.mongo.impl.Wrappers.*;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.*;

import io.quarkus.mongo.*;

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
    public CompletionStage<Document> runCommand(Bson command) {
        return toCompletionStage(database.runCommand(command));
    }

    @Override
    public CompletionStage<Document> runCommand(Bson command, ReadPreference readPreference) {
        return toCompletionStage(database.runCommand(command, readPreference));
    }

    @Override
    public <T> CompletionStage<T> runCommand(Bson command, Class<T> clazz) {
        return toCompletionStage(database.runCommand(command, clazz));
    }

    @Override
    public <T> CompletionStage<T> runCommand(Bson command, ReadPreference readPreference, Class<T> clazz) {
        return toCompletionStage(database.runCommand(command, readPreference, clazz));
    }

    @Override
    public CompletionStage<Document> runCommand(ClientSession clientSession, Bson command) {
        return toCompletionStage(database.runCommand(clientSession, command));
    }

    @Override
    public CompletionStage<Document> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference) {
        return toCompletionStage(database.runCommand(clientSession, command, readPreference));
    }

    @Override
    public <T> CompletionStage<T> runCommand(ClientSession clientSession, Bson command, Class<T> clazz) {
        return toCompletionStage(database.runCommand(clientSession, command, clazz));
    }

    @Override
    public <T> CompletionStage<T> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference,
            Class<T> clazz) {
        return toCompletionStage(database.runCommand(clientSession, command, readPreference, clazz));
    }

    @Override
    public CompletionStage<Void> drop() {
        return toEmptyCompletionStage(database.drop());
    }

    @Override
    public CompletionStage<Void> drop(ClientSession clientSession) {
        return toEmptyCompletionStage(database.drop(clientSession));
    }

    @Override
    public Publisher<String> listCollectionNamesAsPublisher() {
        return database.listCollectionNames();
    }

    @Override
    public Publisher<String> listCollectionNamesAsPublisher(ClientSession clientSession) {
        return database.listCollectionNames(clientSession);
    }

    @Override
    public ListCollectionsPublisher<Document> listCollectionsAsPublisher() {
        return database.listCollections();
    }

    @Override
    public <T> ListCollectionsPublisher<T> listCollectionsAsPublisher(Class<T> clazz) {
        return database.listCollections(clazz);
    }

    @Override
    public ListCollectionsPublisher<Document> listCollectionsAsPublisher(ClientSession clientSession) {
        return database.listCollections(clientSession);
    }

    @Override
    public <T> ListCollectionsPublisher<T> listCollectionsAsPublisher(ClientSession clientSession, Class<T> clazz) {
        return database.listCollections(clientSession, clazz);
    }

    @Override
    public PublisherBuilder<String> listCollectionNames() {
        return toPublisherBuilder(database.listCollectionNames());
    }

    @Override
    public PublisherBuilder<String> listCollectionNames(ClientSession clientSession) {
        return toPublisherBuilder(database.listCollectionNames(clientSession));
    }

    @Override
    public PublisherBuilder<Document> listCollections() {
        return toPublisherBuilder(database.listCollections());
    }

    @Override
    public PublisherBuilder<Document> listCollections(CollectionListOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, database.listCollections()));
    }

    @Override
    public <T> PublisherBuilder<T> listCollections(Class<T> clazz) {
        return toPublisherBuilder(database.listCollections(clazz));
    }

    @Override
    public <T> PublisherBuilder<T> listCollections(Class<T> clazz, CollectionListOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, database.listCollections(clazz)));
    }

    private <T> ListCollectionsPublisher<T> apply(CollectionListOptions options, ListCollectionsPublisher<T> collections) {
        if (options == null) {
            return collections;
        } else {
            return options.apply(collections);
        }
    }

    @Override
    public PublisherBuilder<Document> listCollections(ClientSession clientSession) {
        return toPublisherBuilder(database.listCollections(clientSession));
    }

    @Override
    public PublisherBuilder<Document> listCollections(ClientSession clientSession, CollectionListOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, database.listCollections(clientSession)));
    }

    @Override
    public <T> PublisherBuilder<T> listCollections(ClientSession clientSession, Class<T> clazz) {
        return toPublisherBuilder(database.listCollections(clientSession, clazz));
    }

    @Override
    public <T> PublisherBuilder<T> listCollections(ClientSession clientSession, Class<T> clazz, CollectionListOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, database.listCollections(clientSession, clazz)));
    }

    @Override
    public CompletionStage<Void> createCollection(String collectionName) {
        return toEmptyCompletionStage(database.createCollection(collectionName));
    }

    @Override
    public CompletionStage<Void> createCollection(String collectionName, CreateCollectionOptions options) {
        return toEmptyCompletionStage(database.createCollection(collectionName, options));
    }

    @Override
    public CompletionStage<Void> createCollection(ClientSession clientSession, String collectionName) {
        return toEmptyCompletionStage(database.createCollection(clientSession, collectionName));
    }

    @Override
    public CompletionStage<Void> createCollection(ClientSession clientSession, String collectionName,
            CreateCollectionOptions options) {
        return toEmptyCompletionStage(database.createCollection(clientSession, collectionName, options));
    }

    @Override
    public CompletionStage<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline) {
        return toEmptyCompletionStage(database.createView(viewName, viewOn, pipeline));
    }

    @Override
    public CompletionStage<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions) {
        return toEmptyCompletionStage(database.createView(viewName, viewOn, pipeline, createViewOptions));
    }

    @Override
    public CompletionStage<Void> createView(ClientSession clientSession, String viewName, String viewOn,
            List<? extends Bson> pipeline) {
        return toEmptyCompletionStage(database.createView(clientSession, viewName, viewOn, pipeline));
    }

    @Override
    public CompletionStage<Void> createView(ClientSession clientSession, String viewName, String viewOn,
            List<? extends Bson> pipeline, CreateViewOptions createViewOptions) {
        return toEmptyCompletionStage(database.createView(clientSession, viewName, viewOn, pipeline, createViewOptions));
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher() {
        return database.watch();
    }

    @Override
    public <T> ChangeStreamPublisher<T> watchAsPublisher(Class<T> clazz) {
        return database.watch(clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(List<? extends Bson> pipeline) {
        return database.watch(pipeline);
    }

    @Override
    public <T> ChangeStreamPublisher<T> watchAsPublisher(List<? extends Bson> pipeline, Class<T> clazz) {
        return database.watch(pipeline, clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession) {
        return database.watch(clientSession);
    }

    @Override
    public <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, Class<T> clazz) {
        return database.watch(clientSession, clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline) {
        return database.watch(clientSession, pipeline);
    }

    @Override
    public <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz) {
        return database.watch(clientSession, pipeline, clazz);
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch() {
        return toPublisherBuilder(database.watch());
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz) {
        return toPublisherBuilder(database.watch(clazz));
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline) {
        return toPublisherBuilder(database.watch(pipeline));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz) {
        return toPublisherBuilder(database.watch(pipeline, clazz));
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession) {
        return toPublisherBuilder(database.watch(clientSession));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz) {
        return toPublisherBuilder(database.watch(clientSession, clazz));
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return toPublisherBuilder(database.watch(clientSession, pipeline));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options) {
        return null;
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz) {
        return toPublisherBuilder(database.watch(clientSession, pipeline, clazz));
    }

    @Override
    public <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options) {
        return null;
    }

    @Override
    public AggregatePublisher<Document> aggregateAsPublisher(List<? extends Bson> pipeline) {
        return database.aggregate(pipeline);
    }

    @Override
    public <T> AggregatePublisher<T> aggregateAsPublisher(List<? extends Bson> pipeline, Class<T> clazz) {
        return database.aggregate(pipeline, clazz);
    }

    @Override
    public AggregatePublisher<Document> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline) {
        return database.aggregate(clientSession, pipeline);
    }

    @Override
    public <T> AggregatePublisher<T> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz) {
        return database.aggregate(clientSession, pipeline, clazz);
    }

    @Override
    public PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline) {
        return toPublisherBuilder(database.aggregate(pipeline));
    }

    @Override
    public PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline, AggregateOptions options) {
        return toPublisherBuilder(apply(options, database.aggregate(pipeline)));
    }

    private <T> AggregatePublisher<T> apply(AggregateOptions options, AggregatePublisher<T> aggregate) {
        if (options == null) {
            return aggregate;
        }
        return options.apply(aggregate);
    }

    @Override
    public <T> PublisherBuilder<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz) {
        return toPublisherBuilder(database.aggregate(pipeline, clazz));
    }

    @Override
    public <T> PublisherBuilder<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz, AggregateOptions options) {
        return toPublisherBuilder(apply(options, database.aggregate(pipeline, clazz)));
    }

    @Override
    public PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return toPublisherBuilder(database.aggregate(clientSession, pipeline));
    }

    @Override
    public PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            AggregateOptions options) {
        return toPublisherBuilder(apply(options, database.aggregate(clientSession, pipeline)));
    }

    @Override
    public <T> PublisherBuilder<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz) {
        return toPublisherBuilder(database.aggregate(clientSession, pipeline, clazz));
    }

    @Override
    public <T> PublisherBuilder<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz,
            AggregateOptions options) {
        return toPublisherBuilder(apply(options, database.aggregate(clientSession, pipeline, clazz)));
    }
}
