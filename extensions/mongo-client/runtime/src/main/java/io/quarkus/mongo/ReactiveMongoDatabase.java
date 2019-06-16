package io.quarkus.mongo;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.ListCollectionsPublisher;

/**
 * A reactive API to interact with a Mongo database.
 */
public interface ReactiveMongoDatabase {
    /**
     * Gets the name of the database.
     *
     * @return the database name
     */
    String getName();

    /**
     * Gets a collection.
     *
     * @param collectionName the name of the collection to return
     * @return the collection
     */
    ReactiveMongoCollection<Document> getCollection(String collectionName);

    /**
     * Gets a collection, with a specific default document class.
     *
     * @param collectionName the name of the collection to return
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the collection
     */
    <T> ReactiveMongoCollection<T> getCollection(String collectionName, Class<T> clazz);

    /**
     * Executes command in the context of the current database.
     *
     * @param command the command to be run
     * @return a completion stage containing the command result once completed
     */
    CompletionStage<Document> runCommand(Bson command);

    /**
     * Executes command in the context of the current database.
     *
     * @param command the command to be run
     * @param readPreference the {@link com.mongodb.ReadPreference} to be used when executing the command
     * @return a completion stage containing the command result once completed
     */
    CompletionStage<Document> runCommand(Bson command, ReadPreference readPreference);

    /**
     * Executes command in the context of the current database.
     *
     * @param command the command to be run
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return a completion stage containing the command result once completed
     */
    <T> CompletionStage<T> runCommand(Bson command, Class<T> clazz);

    /**
     * Executes command in the context of the current database.
     *
     * @param command the command to be run
     * @param readPreference the {@link com.mongodb.ReadPreference} to be used when executing the command
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return a completion stage containing the command result once completed
     */
    <T> CompletionStage<T> runCommand(Bson command, ReadPreference readPreference, Class<T> clazz);

    /**
     * Executes command in the context of the current database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param command the command to be run
     * @return a completion stage containing the command result once completed
     */
    CompletionStage<Document> runCommand(ClientSession clientSession, Bson command);

    /**
     * Executes command in the context of the current database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param readPreference the {@link com.mongodb.ReadPreference} to be used when executing the command
     * @param command the command to be run
     * @return a completion stage containing the command result once completed
     */
    CompletionStage<Document> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference);

    /**
     * Executes command in the context of the current database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param command the command to be run
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return a completion stage containing the command result once completed
     */
    <T> CompletionStage<T> runCommand(ClientSession clientSession, Bson command, Class<T> clazz);

    /**
     * Executes command in the context of the current database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param readPreference the {@link com.mongodb.ReadPreference} to be used when executing the command
     * @param command the command to be run
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return a completion stage containing the command result once completed
     */
    <T> CompletionStage<T> runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference, Class<T> clazz);

    /**
     * Drops this database.
     *
     * @return a completion stage completed when the database has been dropped
     */
    CompletionStage<Void> drop();

    /**
     * Drops this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a completion stage completed when the database has been dropped
     */
    CompletionStage<Void> drop(ClientSession clientSession);

    /**
     * Gets a stream of the names of all the collections in this database.
     *
     * @return a stream with all the names of all the collections in this database
     */
    Publisher<String> listCollectionNamesAsPublisher();

    /**
     * Gets a stream of the names of all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a stream with all the names of all the collections in this database
     */
    Publisher<String> listCollectionNamesAsPublisher(ClientSession clientSession);

    /**
     * Finds all the collections in this database.
     *
     * @return the stream of collection descriptor
     */
    ListCollectionsPublisher<Document> listCollectionsAsPublisher();

    /**
     * Finds all the collections in this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of collection descriptor
     */
    <T> ListCollectionsPublisher<T> listCollectionsAsPublisher(Class<T> clazz);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of collection descriptor
     */
    ListCollectionsPublisher<Document> listCollectionsAsPublisher(ClientSession clientSession);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return stream of collection descriptor
     */
    <T> ListCollectionsPublisher<T> listCollectionsAsPublisher(ClientSession clientSession, Class<T> clazz);

    /**
     * Gets a stream of the names of all the collections in this database.
     *
     * @return a stream with all the names of all the collections in this database
     */
    PublisherBuilder<String> listCollectionNames();

    /**
     * Gets a stream of the names of all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a stream with all the names of all the collections in this database
     */
    PublisherBuilder<String> listCollectionNames(ClientSession clientSession);

    /**
     * Finds all the collections in this database.
     *
     * @return stream of collection descriptor
     */
    PublisherBuilder<Document> listCollections();

    /**
     * Finds all the collections in this database.
     *
     * @param options the stream options
     * @return stream of collection descriptor
     */
    PublisherBuilder<Document> listCollections(CollectionListOptions options);

    /**
     * Finds all the collections in this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return stream of collection descriptor
     */
    <T> PublisherBuilder<T> listCollections(Class<T> clazz);

    /**
     * Finds all the collections in this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return stream of collection descriptor
     */
    <T> PublisherBuilder<T> listCollections(Class<T> clazz, CollectionListOptions options);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return stream of collection descriptor
     */
    PublisherBuilder<Document> listCollections(ClientSession clientSession);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return stream of collection descriptor
     */
    PublisherBuilder<Document> listCollections(ClientSession clientSession, CollectionListOptions options);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return stream of collection descriptor
     */
    <T> PublisherBuilder<T> listCollections(ClientSession clientSession, Class<T> clazz);

    /**
     * Finds all the collections in this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return stream of collection descriptor
     */
    <T> PublisherBuilder<T> listCollections(ClientSession clientSession, Class<T> clazz, CollectionListOptions options);

    /**
     * Create a new collection with the selected options
     *
     * @param collectionName the name for the new collection to create
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createCollection(String collectionName);

    /**
     * Create a new collection with the selected options
     *
     * @param collectionName the name for the new collection to create
     * @param options various options for creating the collection
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createCollection(String collectionName, CreateCollectionOptions options);

    /**
     * Create a new collection with the selected options
     *
     * @param clientSession the client session with which to associate this operation
     * @param collectionName the name for the new collection to create
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createCollection(ClientSession clientSession, String collectionName);

    /**
     * Create a new collection with the selected options
     *
     * @param clientSession the client session with which to associate this operation
     * @param collectionName the name for the new collection to create
     * @param options various options for creating the collection
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createCollection(ClientSession clientSession, String collectionName, CreateCollectionOptions options);

    /**
     * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that
     * defines the view.
     *
     * @param viewName the name of the view to create
     * @param viewOn the backing collection/view for the view
     * @param pipeline the pipeline that defines the view
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline);

    /**
     * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that
     * defines the view.
     *
     * @param viewName the name of the view to create
     * @param viewOn the backing collection/view for the view
     * @param pipeline the pipeline that defines the view
     * @param createViewOptions various options for creating the view
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createView(String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions);

    /**
     * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that
     * defines the view.
     *
     * @param clientSession the client session with which to associate this operation
     * @param viewName the name of the view to create
     * @param viewOn the backing collection/view for the view
     * @param pipeline the pipeline that defines the view
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createView(ClientSession clientSession, String viewName, String viewOn,
            List<? extends Bson> pipeline);

    /**
     * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that
     * defines the view.
     *
     * @param clientSession the client session with which to associate this operation
     * @param viewName the name of the view to create
     * @param viewOn the backing collection/view for the view
     * @param pipeline the pipeline that defines the view
     * @param createViewOptions various options for creating the view
     * @return a completion stage completed when the operation has completed
     */
    CompletionStage<Void> createView(ClientSession clientSession, String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions);

    /**
     * Creates a change stream for this database.
     *
     * @return the stream of change events.
     */
    ChangeStreamPublisher<Document> watchAsPublisher();

    /**
     * Creates a change stream for this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change events.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of change events.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change events.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch();

    /**
     * Creates a change stream for this database.
     *
     * @param options the stream options
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change events.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz);

    /**
     * Creates a change stream for this database.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable
     * @param options the stream options
     * @return the stream of change events.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @return a stream of the result of the aggregation operation
     */
    AggregatePublisher<Document> aggregateAsPublisher(List<? extends Bson> pipeline);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return a stream of the result of the aggregation operation
     */
    <T> AggregatePublisher<T> aggregateAsPublisher(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @return a stream of the result of the aggregation operation
     */
    AggregatePublisher<Document> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return a stream of the result of the aggregation operation
     */
    <T> AggregatePublisher<T> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @return a stream of the result of the aggregation operation
     */
    PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @param options the stream options
     * @return a stream of the result of the aggregation operation
     */
    PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline, AggregateOptions options);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return a stream of the result of the aggregation operation
     */
    <T> PublisherBuilder<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return a stream of the result of the aggregation operation
     */
    <T> PublisherBuilder<T> aggregate(List<? extends Bson> pipeline, Class<T> clazz, AggregateOptions options);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @return a stream of the result of the aggregation operation
     */
    PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @param options the stream options
     * @return a stream of the result of the aggregation operation
     */
    PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, AggregateOptions options);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return a stream of the result of the aggregation operation
     */
    <T> PublisherBuilder<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Runs an aggregation framework pipeline on the database for pipeline stages
     * that do not require an underlying collection, such as {@code $currentOp} and {@code $listLocalSessions}.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return a stream of the result of the aggregation operation
     */
    <T> PublisherBuilder<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz,
            AggregateOptions options);
}
