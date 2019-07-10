package io.quarkus.mongodb;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.ListDatabasesPublisher;

/**
 * A reactive Mongo client.
 * Instances can represent either a standalone MongoDB instance, a replica set, or a sharded cluster. Instance of this
 * class are responsible for maintaining an up-to-date state of the cluster, and possibly cache resources related to
 * this, including background threads for monitoring, and connection pools.
 * <p>
 * Instance of this class server as factories for {@code ReactiveMongoDatabase} instances.
 * </p>
 */
public interface ReactiveMongoClient extends Closeable {

    /**
     * Retrieves a {@link ReactiveMongoDatabase} with the given name.
     *
     * @param name the name, must not be {@code null}
     * @return the {@link ReactiveMongoDatabase}
     */
    ReactiveMongoDatabase getDatabase(String name);

    /**
     * Closes the client, which will close all underlying cached resources, including, for example,
     * sockets and background monitoring threads.
     */
    @Override
    void close();

    /**
     * Gets a list of the database names.
     *
     * @return a stream containing the database names, empty is none.
     */
    PublisherBuilder<String> listDatabaseNames();

    /**
     * Gets a list of the database names.
     *
     * @return a stream containing the database names, empty is none.
     */
    Publisher<String> listDatabaseNamesAsPublisher();

    /**
     * Gets a list of the database names.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a stream containing the database names, empty is none.
     */
    PublisherBuilder<String> listDatabaseNames(ClientSession clientSession);

    /**
     * Gets a list of the database names.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a stream containing the database names, empty is none.
     */
    Publisher<String> listDatabaseNamesAsPublisher(ClientSession clientSession);

    /**
     * Gets the list of database descriptors.
     *
     * @return a configurable stream of the database.
     */
    ListDatabasesPublisher<Document> listDatabasesAsPublisher();

    /**
     * Gets the list of database descriptors.
     *
     * @return a stream of the database, empty if none.
     */
    PublisherBuilder<Document> listDatabases();

    /**
     * Gets the list of database descriptors.
     *
     * @param options the stream options (max time, filter, name only...), may be {@code null}
     * @return a stream of the database, empty if none.
     */
    PublisherBuilder<Document> listDatabases(DatabaseListOptions options);

    /**
     * Gets the list of databases.
     *
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the fluent list databases interface
     */
    <T> ListDatabasesPublisher<T> listDatabasesAsPublisher(Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the stream of database descriptors
     */
    <T> PublisherBuilder<T> listDatabases(Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @param options the stream options
     * @return the stream of database descriptors
     */
    <T> PublisherBuilder<T> listDatabases(Class<T> clazz, DatabaseListOptions options);

    /**
     * Gets the list of databases as a stream.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the fluent list databases interface
     */
    ListDatabasesPublisher<Document> listDatabasesAsPublisher(ClientSession clientSession);

    /**
     * Gets the list of databases as a stream.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of database descriptors, empty if none.
     */
    PublisherBuilder<Document> listDatabases(ClientSession clientSession);

    /**
     * Gets the list of databases as a stream.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of database descriptors, empty if none.
     */
    PublisherBuilder<Document> listDatabases(ClientSession clientSession, DatabaseListOptions options);

    /**
     * Gets the list of databases.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the fluent list databases interface
     */
    <T> ListDatabasesPublisher<T> listDatabasesAsPublisher(ClientSession clientSession, Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the stream of database descriptors
     */
    <T> PublisherBuilder<T> listDatabases(ClientSession clientSession, Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @param options the stream options
     * @return the stream of database descriptors
     */
    <T> PublisherBuilder<T> listDatabases(ClientSession clientSession, Class<T> clazz, DatabaseListOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @return the stream of change stream.
     */
    ChangeStreamPublisher<Document> watchAsPublisher();

    /**
     * Creates a change stream for this client.
     *
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch();

    /**
     * Creates a change stream for this client.
     *
     * @param options the stream options
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of change stream.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change stream.
     */
    PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream
     */
    <T> ChangeStreamPublisher<T> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> PublisherBuilder<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options);

    /**
     * Creates a client session.
     *
     * @return a {@link CompletionStage} completed when the session is ready to be used.
     */
    CompletionStage<ClientSession> startSession();

    /**
     * Creates a client session.
     *
     * @param options the options for the client session
     * @return a {@link CompletionStage} completed when the session is ready to be used.
     */
    CompletionStage<ClientSession> startSession(ClientSessionOptions options);
}
