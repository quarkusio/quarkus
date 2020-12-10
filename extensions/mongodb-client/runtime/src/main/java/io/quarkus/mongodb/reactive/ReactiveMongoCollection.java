package io.quarkus.mongodb.reactive;

import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.EstimatedDocumentCountOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.DistinctOptions;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.MapReduceOptions;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * A reactive API to interact with a Mongo collection.
 *
 * @param <T> The type that this collection will encode documents from and decode documents to.
 * @since 1.0
 */
public interface ReactiveMongoCollection<T> {

    /**
     * Gets the namespace of this collection.
     *
     * @return the namespace
     */
    MongoNamespace getNamespace();

    /**
     * Get the class of documents stored in this collection.
     *
     * @return the class
     */
    Class<T> getDocumentClass();

    /**
     * Gets an estimate of the count of documents in a collection using collection metadata.
     *
     * @return a {@link Uni} completed with the estimated number of documents
     */
    Uni<Long> estimatedDocumentCount();

    /**
     * Gets an estimate of the count of documents in a collection using collection metadata.
     *
     * @param options the options describing the count
     * @return a {@link Uni} completed with the estimated number of documents
     */
    Uni<Long> estimatedDocumentCount(EstimatedDocumentCountOptions options);

    /**
     * Counts the number of documents in the collection.
     *
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments();

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * @param filter the query filter
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments(Bson filter);

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * @param filter the query filter
     * @param options the options describing the count
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments(Bson filter, CountOptions options);

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments(ClientSession clientSession);

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments(ClientSession clientSession, Bson filter);

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @param options the options describing the count
     * @return a {@link Uni} completed with the number of documents
     */
    Uni<Long> countDocuments(ClientSession clientSession, Bson filter, CountOptions options);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName the field name*
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(String fieldName, Class<D> clazz);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName the field name
     * @param filter the query filter
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(String fieldName, Bson filter, Class<D> clazz);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName the field name
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName the field name
     * @param filter the query filter
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName the field name
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @param options the stream options
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(String fieldName, Class<D> clazz, DistinctOptions options);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName the field name
     * @param filter the query filter
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @param options the stream options
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(String fieldName, Bson filter, Class<D> clazz, DistinctOptions options);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName the field name
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @param options the stream options
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz,
            DistinctOptions options);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName the field name
     * @param filter the query filter
     * @param clazz the default class to cast any distinct items into.
     * @param <D> the target type of the iterable.
     * @param options the stream options
     * @return a {@link Multi} emitting the sequence of distinct values
     */
    <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz,
            DistinctOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find();

    /**
     * Finds all documents in the collection.
     *
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(Class<D> clazz);

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(Bson filter);

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(Bson filter, Class<D> clazz);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(ClientSession clientSession);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(ClientSession clientSession, Class<D> clazz);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(ClientSession clientSession, Bson filter);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(ClientSession clientSession, Bson filter, Class<D> clazz);

    /**
     * Finds all documents in the collection.
     *
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(Class<D> clazz, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(Bson filter, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(Bson filter, Class<D> clazz, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(ClientSession clientSession, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(ClientSession clientSession, Class<D> clazz, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    Multi<T> find(ClientSession clientSession, Bson filter, FindOptions options);

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream with the selected documents, can be empty if none matches.
     */
    <D> Multi<D> find(ClientSession clientSession, Bson filter, Class<D> clazz, FindOptions options);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregate pipeline
     * @return a stream containing the result of the aggregation operation
     */
    Multi<T> aggregate(List<? extends Bson> pipeline);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregate pipeline
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return a stream containing the result of the aggregation operation
     */
    <D> Multi<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregate pipeline
     * @return a stream containing the result of the aggregation operation
     */
    Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregate pipeline
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return a stream containing the result of the aggregation operation
     */
    <D> Multi<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz);

    //

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregate pipeline
     * @param options the stream options
     * @return a stream containing the result of the aggregation operation
     */
    Multi<T> aggregate(List<? extends Bson> pipeline, AggregateOptions options);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregate pipeline
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return a stream containing the result of the aggregation operation
     */
    <D> Multi<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz, AggregateOptions options);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregate pipeline
     * @param options the stream options
     * @return a stream containing the result of the aggregation operation
     */
    Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            AggregateOptions options);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregate pipeline
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return a stream containing the result of the aggregation operation
     */
    <D> Multi<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz,
            AggregateOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch();

    /**
     * Creates a change stream for this collection.
     *
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(Class<D> clazz);

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz);

    /**
     * Creates a change stream for this collection.
     *
     * @param options the stream options
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(Class<D> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param options the stream options
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param options the stream options
     * @return the stream of changes
     */
    Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options);

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregation pipeline to apply to the change stream
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @param options the stream options
     * @return the stream of changes
     */
    <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz, ChangeStreamOptions options);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    Multi<T> mapReduce(String mapFunction, String reduceFunction);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param clazz the class to decode each resulting document into.
     * @param <D> the target document type of the iterable.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    <D> Multi<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession the client session with which to associate this operation
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    Multi<T> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession the client session with which to associate this operation
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param clazz the class to decode each resulting document into.
     * @param <D> the target document type of the iterable.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    <D> Multi<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param options The map reduce options configuring process and result.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    Multi<T> mapReduce(String mapFunction, String reduceFunction, MapReduceOptions options);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param clazz the class to decode each resulting document into.
     * @param options The map reduce options configuring process and result.
     * @param <D> the target document type of the iterable.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    <D> Multi<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz,
            MapReduceOptions options);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession the client session with which to associate this operation
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param options The map reduce options configuring process and result.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    Multi<T> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            MapReduceOptions options);

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession the client session with which to associate this operation
     * @param mapFunction A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular
     *        key.
     * @param clazz the class to decode each resulting document into.
     * @param options The map reduce options configuring process and result.
     * @param <D> the target document type of the iterable.
     * @return a {@link Multi} containing the result of the map-reduce operation
     */
    <D> Multi<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz,
            MapReduceOptions options);

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * @param requests the writes to execute
     * @return a {@link Uni} receiving the {@link BulkWriteResult}
     */
    Uni<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests);

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * @param requests the writes to execute
     * @param options the options to apply to the bulk write operation
     * @return a {@link Uni} receiving the {@link BulkWriteResult}
     */
    Uni<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests,
            BulkWriteOptions options);

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * @param clientSession the client session with which to associate this operation
     * @param requests the writes to execute
     * @return a {@link Uni} receiving the {@link BulkWriteResult}
     */
    Uni<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests);

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * @param clientSession the client session with which to associate this operation
     * @param requests the writes to execute
     * @param options the options to apply to the bulk write operation
     * @return a {@link Uni} receiving the {@link BulkWriteResult}
     */
    Uni<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests,
            BulkWriteOptions options);

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * @param document the document to insert
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertOneResult> insertOne(T document);

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * @param document the document to insert
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertOneResult> insertOne(T document, InsertOneOptions options);

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * @param clientSession the client session with which to associate this operation
     * @param document the document to insert
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertOneResult> insertOne(ClientSession clientSession, T document);

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * @param clientSession the client session with which to associate this operation
     * @param document the document to insert
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertOneResult> insertOne(ClientSession clientSession, T document, InsertOneOptions options);

    /**
     * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
     *
     * @param documents the documents to insert
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertManyResult> insertMany(List<? extends T> documents);

    /**
     * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
     *
     * @param documents the documents to insert
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertManyResult> insertMany(List<? extends T> documents, InsertManyOptions options);

    /**
     * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
     *
     * @param clientSession the client session with which to associate this operation
     * @param documents the documents to insert
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertManyResult> insertMany(ClientSession clientSession, List<? extends T> documents);

    /**
     * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
     *
     * @param clientSession the client session with which to associate this operation
     * @param documents the documents to insert
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed successfully when the operation completes, or propagating a
     *         {@link com.mongodb.DuplicateKeyException} or {@link com.mongodb.MongoException} on failure.
     */
    Uni<InsertManyResult> insertMany(ClientSession clientSession, List<? extends T> documents,
            InsertManyOptions options);

    /**
     * Removes at most one document from the collection that matches the given filter.
     * If no documents match, the collection is not modified.
     *
     * @param filter the query filter to apply the the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteOne(Bson filter);

    /**
     * Removes at most one document from the collection that matches the given filter.
     * If no documents match, the collection is not modified.
     *
     * @param filter the query filter to apply the the delete operation
     * @param options the options to apply to the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteOne(Bson filter, DeleteOptions options);

    /**
     * Removes at most one document from the collection that matches the given filter.
     * If no documents match, the collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteOne(ClientSession clientSession, Bson filter);

    /**
     * Removes at most one document from the collection that matches the given filter.
     * If no documents match, the collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the delete operation
     * @param options the options to apply to the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options);

    /**
     * Removes all documents from the collection that match the given query filter. If no documents match, the
     * collection is not modified.
     *
     * @param filter the query filter to apply the the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteMany(Bson filter);

    /**
     * Removes all documents from the collection that match the given query filter. If no documents match, the
     * collection is not modified.
     *
     * @param filter the query filter to apply the the delete operation
     * @param options the options to apply to the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteMany(Bson filter, DeleteOptions options);

    /**
     * Removes all documents from the collection that match the given query filter. If no documents match, the
     * collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteMany(ClientSession clientSession, Bson filter);

    /**
     * Removes all documents from the collection that match the given query filter. If no documents match, the
     * collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the delete operation
     * @param options the options to apply to the delete operation
     * @return a {@link Uni} receiving the {@link DeleteResult}, or propagating a {@link com.mongodb.MongoException} on
     *         failure.
     */
    Uni<DeleteResult> deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> replaceOne(Bson filter, T replacement);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @param options the options to apply to the replace operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> replaceOne(Bson filter, T replacement, ReplaceOptions options);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @param options the options to apply to the replace operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement,
            ReplaceOptions options);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateOne(Bson filter, Bson update);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the update operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateOne(Bson filter, Bson update, UpdateOptions options);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the update operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateMany(Bson filter, Bson update);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the update operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateMany(Bson filter, Bson update, UpdateOptions options);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the update operation
     * @return a {@link Uni} receiving the {@link UpdateResult}
     */
    Uni<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options);

    /**
     * Atomically find a document and remove it.
     *
     * @param filter the query filter to find the document with
     * @return a {@link Uni} completed with the document that was removed. If no documents matched the query filter,
     *         then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndDelete(Bson filter);

    /**
     * Atomically find a document and remove it.
     *
     * @param filter the query filter to find the document with
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was removed. If no documents matched the query filter,
     *         then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndDelete(Bson filter, FindOneAndDeleteOptions options);

    /**
     * Atomically find a document and remove it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to find the document with
     * @return a {@link Uni} completed with the document that was removed. If no documents matched the query filter,
     *         then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndDelete(ClientSession clientSession, Bson filter);

    /**
     * Atomically find a document and remove it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to find the document with
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was removed. If no documents matched the query filter,
     *         then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options);

    /**
     * Atomically find a document and replace it.
     *
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @return a {@link Uni} completed with the document that was replaced. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndReplace(Bson filter, T replacement);

    /**
     * Atomically find a document and replace it.
     *
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was replaced. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndReplace(Bson filter, T replacement, FindOneAndReplaceOptions options);

    /**
     * Atomically find a document and replace it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @return a {@link Uni} completed with the document that was replaced. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement);

    /**
     * Atomically find a document and replace it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter the query filter to apply the the replace operation
     * @param replacement the replacement document
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was replaced. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement,
            FindOneAndReplaceOptions options);

    /**
     * Atomically find a document and update it.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} completed with the document that was updated. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndUpdate(Bson filter, Bson update);

    /**
     * Atomically find a document and update it.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was updated. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options);

    /**
     * Atomically find a document and update it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @return a {@link Uni} completed with the document that was updated. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update);

    /**
     * Atomically find a document and update it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update
     *        operators.
     * @param options the options to apply to the operation
     * @return a {@link Uni} completed with the document that was updated. Depending on the value of the
     *         {@code returnOriginal}
     *         property, this will either be the document as it was before the update or as it is after the update. If no
     *         documents matched the
     *         query filter, then the uni is completed with {@code null}.
     */
    Uni<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update,
            FindOneAndUpdateOptions options);

    /**
     * Drops this collection from the database.
     *
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> drop();

    /**
     * Drops this collection from the database.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> drop(ClientSession clientSession);

    /**
     * Creates an index.
     *
     * @param key an object describing the index key(s), which may not be null.
     * @return a {@link Uni} receiving the created index name.
     */
    Uni<String> createIndex(Bson key);

    /**
     * Creates an index.
     *
     * @param key an object describing the index key(s), which may not be null.
     * @param options the options for the index
     * @return a {@link Uni} receiving the created index name.
     */
    Uni<String> createIndex(Bson key, IndexOptions options);

    /**
     * Creates an index.
     *
     * @param clientSession the client session with which to associate this operation
     * @param key an object describing the index key(s), which may not be null.
     * @return a {@link Uni} receiving the created index name.
     */
    Uni<String> createIndex(ClientSession clientSession, Bson key);

    /**
     * Creates an index.
     *
     * @param clientSession the client session with which to associate this operation
     * @param key an object describing the index key(s), which may not be null.
     * @param options the options for the index
     * @return a {@link Uni} receiving the created index name.
     */
    Uni<String> createIndex(ClientSession clientSession, Bson key, IndexOptions options);

    /**
     * Create multiple indexes.
     *
     * @param indexes the list of indexes
     * @return a {@link Uni} completed with the result when the operation is done. The redeemed list contains the
     *         created index names.
     */
    Uni<List<String>> createIndexes(List<IndexModel> indexes);

    /**
     * Create multiple indexes.
     *
     * @param indexes the list of indexes
     * @param createIndexOptions options to use when creating indexes
     * @return a {@link Uni} completed with the result when the operation is done. The redeemed list contains the
     *         created index names.
     */
    Uni<List<String>> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions);

    /**
     * Create multiple indexes.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexes the list of indexes
     * @return a {@link Uni} completed with the result when the operation is done. The redeemed list contains the
     *         created index names.
     */
    Uni<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes);

    /**
     * Create multiple indexes.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexes the list of indexes
     * @param createIndexOptions options to use when creating indexes
     * @return a {@link Uni} completed with the result when the operation is done. The redeemed list contains the
     *         created index names.
     */
    Uni<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes,
            CreateIndexOptions createIndexOptions);

    /**
     * Get all the indexes in this collection.
     *
     * @return the stream of indexes
     */
    Multi<Document> listIndexes();

    /**
     * Get all the indexes in this collection.
     *
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of indexes
     */
    <D> Multi<D> listIndexes(Class<D> clazz);

    /**
     * Get all the indexes in this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the stream of indexes
     */
    Multi<Document> listIndexes(ClientSession clientSession);

    /**
     * Get all the indexes in this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return the stream of indexes
     */
    <D> Multi<D> listIndexes(ClientSession clientSession, Class<D> clazz);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param indexName the name of the index to remove
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(String indexName);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param keys the keys of the index to remove
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(Bson keys);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param indexName the name of the index to remove
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(String indexName, DropIndexOptions dropIndexOptions);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param keys the keys of the index to remove
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(Bson keys, DropIndexOptions dropIndexOptions);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexName the name of the index to remove
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(ClientSession clientSession, String indexName);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param keys the keys of the index to remove
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(ClientSession clientSession, Bson keys);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexName the name of the index to remove
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions);

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param keys the keys of the index to remove
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions);

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndexes();

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndexes(DropIndexOptions dropIndexOptions);

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param clientSession the client session with which to associate this operation
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndexes(ClientSession clientSession);

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param clientSession the client session with which to associate this operation
     * @param dropIndexOptions options to use when dropping indexes
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions);

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param newCollectionNamespace the name the collection will be renamed to
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> renameCollection(MongoNamespace newCollectionNamespace);

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param newCollectionNamespace the name the collection will be renamed to
     * @param options the options for renaming a collection
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions options);

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param clientSession the client session with which to associate this operation
     * @param newCollectionNamespace the name the collection will be renamed to
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace);

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param clientSession the client session with which to associate this operation
     * @param newCollectionNamespace the name the collection will be renamed to
     * @param options the options for renaming a collection
     * @return a {@link Uni} completed when the operation is done.
     */
    Uni<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace,
            RenameCollectionOptions options);

    /**
     * Gets the codec registry of this collection.
     *
     * @return the codec registry
     */
    CodecRegistry getCodecRegistry();

    /**
     * Create a new ReactiveMongoCollection instance with a different default class to cast any documents returned from the
     * database into..
     *
     * @param clazz the default class to cast any documents returned from the database into.
     * @param <NewTDocument> The type that the new collection will encode documents from and decode documents to
     * @return a new ReactiveMongoCollection instance with the different default class
     */
    <NewTDocument> ReactiveMongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz);

    /**
     * Create a new ReactiveMongoCollection instance with a different read preference.
     *
     * @param readPreference the new {@link com.mongodb.ReadPreference} for the collection
     * @return a new ReactiveMongoCollection instance with the different readPreference
     */
    ReactiveMongoCollection<T> withReadPreference(ReadPreference readPreference);
}
