package io.quarkus.mongodb.reactive;

import java.io.Closeable;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;

import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.DatabaseListOptions;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

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
    Multi<String> listDatabaseNames();

    /**
     * Gets a list of the database names.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a stream containing the database names, empty is none.
     */
    Multi<String> listDatabaseNames(ClientSession clientSession);

    /**
     * Gets the list of database descriptors.
     *
     * @return a stream of the database, empty if none.
     */
    Multi<Document> listDatabases();

    /**
     * Gets the list of database descriptors.
     *
     * @param options the stream options (max time, filter, name only...), may be {@code null}
     * @return a stream of the database, empty if none.
     */
    Multi<Document> listDatabases(DatabaseListOptions options);

    /**
     * Gets the list of databases.
     *
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the stream of database descriptors
     */
    <T> Multi<T> listDatabases(Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @param options the stream options
     * @return the stream of database descriptors
     */
    <T> Multi<T> listDatabases(Class<T> clazz, DatabaseListOptions options);

    /**
     * Gets the list of databases as a stream.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of database descriptors, empty if none.
     */
    Multi<Document> listDatabases(ClientSession clientSession);

    /**
     * Gets the list of databases as a stream.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of database descriptors, empty if none.
     */
    Multi<Document> listDatabases(ClientSession clientSession, DatabaseListOptions options);

    /**
     * Gets the list of databases.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @return the stream of database descriptors
     */
    <T> Multi<T> listDatabases(ClientSession clientSession, Class<T> clazz);

    /**
     * Gets the list of databases.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to cast the database documents to
     * @param <T> the type of the class to use instead of {@code Document}.
     * @param options the stream options
     * @return the stream of database descriptors
     */
    <T> Multi<T> listDatabases(ClientSession clientSession, Class<T> clazz, DatabaseListOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch();

    /**
     * Creates a change stream for this client.
     *
     * @param options the stream options
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(Class<T> clazz, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(List<? extends Bson> pipeline, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, Class<T> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of change stream.
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this client.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <T> the target document type of the iterable.
     * @return the stream of change stream.
     */
    <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
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
    <T> Multi<ChangeStreamDocument<T>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<T> clazz, ChangeStreamOptions options);

    /**
     * Creates a client session.
     *
     * @return a {@link Uni} completed when the session is ready to be used.
     */
    Uni<ClientSession> startSession();

    /**
     * Creates a client session.
     *
     * @param options the options for the client session
     * @return a {@link Uni} completed when the session is ready to be used.
     */
    Uni<ClientSession> startSession(ClientSessionOptions options);

    /**
     * @return the underlying client.
     */
    MongoClient unwrap();
}
