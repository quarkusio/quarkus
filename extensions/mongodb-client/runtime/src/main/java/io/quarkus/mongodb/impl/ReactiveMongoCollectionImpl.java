package io.quarkus.mongodb.impl;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import com.mongodb.MongoNamespace;
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
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.DistinctPublisher;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.ListIndexesPublisher;
import com.mongodb.reactivestreams.client.MapReducePublisher;
import com.mongodb.reactivestreams.client.MongoCollection;

import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.DistinctOptions;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.MapReduceOptions;
import io.quarkus.mongodb.ReactiveMongoCollection;

public class ReactiveMongoCollectionImpl<T> implements ReactiveMongoCollection<T> {

    private final MongoCollection<T> collection;

    ReactiveMongoCollectionImpl(MongoCollection<T> collection) {
        this.collection = collection;
    }

    @Override
    public MongoNamespace getNamespace() {
        return collection.getNamespace();
    }

    @Override
    public Class<T> getDocumentClass() {
        return collection.getDocumentClass();
    }

    @Override
    public CompletionStage<Long> estimatedDocumentCount() {
        return Wrappers.toCompletionStage(collection.estimatedDocumentCount());
    }

    @Override
    public CompletionStage<Long> estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return Wrappers.toCompletionStage(collection.estimatedDocumentCount(options));
    }

    @Override
    public CompletionStage<Long> countDocuments() {
        return Wrappers.toCompletionStage(collection.countDocuments());
    }

    @Override
    public CompletionStage<Long> countDocuments(Bson filter) {
        return Wrappers.toCompletionStage(collection.countDocuments(filter));
    }

    @Override
    public CompletionStage<Long> countDocuments(Bson filter, CountOptions options) {
        return Wrappers.toCompletionStage(collection.countDocuments(filter, options));
    }

    @Override
    public CompletionStage<Long> countDocuments(ClientSession clientSession) {
        return Wrappers.toCompletionStage(collection.countDocuments(clientSession));
    }

    @Override
    public CompletionStage<Long> countDocuments(ClientSession clientSession, Bson filter) {
        return Wrappers.toCompletionStage(collection.countDocuments(clientSession, filter));
    }

    @Override
    public CompletionStage<Long> countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return Wrappers.toCompletionStage(collection.countDocuments(clientSession, filter, options));
    }

    @Override
    public <D> DistinctPublisher<D> distinctAsPublisher(String fieldName, Class<D> clazz) {
        return collection.distinct(fieldName, clazz);
    }

    @Override
    public <D> DistinctPublisher<D> distinctAsPublisher(String fieldName, Bson filter, Class<D> clazz) {
        return collection.distinct(fieldName, filter, clazz);
    }

    @Override
    public <D> DistinctPublisher<D> distinctAsPublisher(ClientSession clientSession, String fieldName, Class<D> clazz) {
        return collection.distinct(clientSession, fieldName, clazz);
    }

    @Override
    public <D> DistinctPublisher<D> distinctAsPublisher(ClientSession clientSession, String fieldName, Bson filter,
            Class<D> clazz) {
        return collection.distinct(clientSession, fieldName, filter, clazz);
    }

    @Override
    public <D> PublisherBuilder<D> distinct(String fieldName, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.distinct(fieldName, clazz));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(String fieldName, Bson filter, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.distinct(fieldName, filter, clazz));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.distinct(clientSession, fieldName, clazz));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.distinct(clientSession, fieldName, filter, clazz));
    }

    private <D> DistinctPublisher<D> apply(DistinctOptions options, DistinctPublisher<D> stream) {
        if (options == null) {
            return stream;
        }
        return options.apply(stream);
    }

    @Override
    public <D> PublisherBuilder<D> distinct(String fieldName, Class<D> clazz, DistinctOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.distinct(fieldName, clazz)));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(String fieldName, Bson filter, Class<D> clazz, DistinctOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.distinct(fieldName, filter, clazz)));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz,
            DistinctOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.distinct(clientSession, fieldName, clazz)));
    }

    @Override
    public <D> PublisherBuilder<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz,
            DistinctOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.distinct(clientSession, fieldName, filter, clazz)));
    }

    @Override
    public FindPublisher<T> findAsPublisher() {
        return collection.find();
    }

    @Override
    public <D> FindPublisher<D> findAsPublisher(Class<D> clazz) {
        return collection.find(clazz);
    }

    @Override
    public FindPublisher<T> findAsPublisher(Bson filter) {
        return collection.find(filter);
    }

    @Override
    public <D> FindPublisher<D> findAsPublisher(Bson filter, Class<D> clazz) {
        return collection.find(filter, clazz);
    }

    @Override
    public FindPublisher<T> findAsPublisher(ClientSession clientSession) {
        return collection.find(clientSession);
    }

    @Override
    public <D> FindPublisher<D> findAsPublisher(ClientSession clientSession, Class<D> clazz) {
        return collection.find(clientSession, clazz);
    }

    @Override
    public FindPublisher<T> findAsPublisher(ClientSession clientSession, Bson filter) {
        return collection.find(clientSession, filter);
    }

    @Override
    public <D> FindPublisher<D> findAsPublisher(ClientSession clientSession, Bson filter, Class<D> clazz) {
        return collection.find(clientSession, filter, clazz);
    }

    @Override
    public PublisherBuilder<T> find() {
        return Wrappers.toPublisherBuilder(collection.find());
    }

    @Override
    public <D> PublisherBuilder<D> find(Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.find(clazz));
    }

    @Override
    public PublisherBuilder<T> find(Bson filter) {
        return Wrappers.toPublisherBuilder(collection.find(filter));
    }

    @Override
    public <D> PublisherBuilder<D> find(Bson filter, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.find(filter, clazz));
    }

    @Override
    public PublisherBuilder<T> find(ClientSession clientSession) {
        return Wrappers.toPublisherBuilder(collection.find(clientSession));
    }

    @Override
    public <D> PublisherBuilder<D> find(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.find(clientSession, clazz));
    }

    @Override
    public PublisherBuilder<T> find(ClientSession clientSession, Bson filter) {
        return Wrappers.toPublisherBuilder(collection.find(clientSession, filter));
    }

    @Override
    public <D> PublisherBuilder<D> find(ClientSession clientSession, Bson filter, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.find(clientSession, filter, clazz));
    }

    private <D> FindPublisher<D> apply(io.quarkus.mongodb.FindOptions options, FindPublisher<D> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    @Override
    public PublisherBuilder<T> find(io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find()));
    }

    @Override
    public <D> PublisherBuilder<D> find(Class<D> clazz, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(clazz)));
    }

    @Override
    public PublisherBuilder<T> find(Bson filter, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(filter)));
    }

    @Override
    public <D> PublisherBuilder<D> find(Bson filter, Class<D> clazz, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(filter, clazz)));
    }

    @Override
    public PublisherBuilder<T> find(ClientSession clientSession, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(clientSession)));
    }

    @Override
    public <D> PublisherBuilder<D> find(ClientSession clientSession, Class<D> clazz, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(clientSession, clazz)));
    }

    @Override
    public PublisherBuilder<T> find(ClientSession clientSession, Bson filter, io.quarkus.mongodb.FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(clientSession, filter)));
    }

    @Override
    public <D> PublisherBuilder<D> find(ClientSession clientSession, Bson filter, Class<D> clazz, FindOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.find(clientSession, filter, clazz)));
    }

    @Override
    public AggregatePublisher<Document> aggregateAsPublisher(List<? extends Bson> pipeline) {
        return collection.aggregate(pipeline);
    }

    @Override
    public <D> AggregatePublisher<D> aggregateAsPublisher(List<? extends Bson> pipeline, Class<D> clazz) {
        return collection.aggregate(pipeline, clazz);
    }

    @Override
    public AggregatePublisher<Document> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline) {
        return collection.aggregate(clientSession, pipeline);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline the aggregate pipeline
     * @param clazz the class to decode each document into
     * @param <D> the target document type of the iterable.
     * @return a stream containing the result of the aggregation operation
     */
    @Override
    public <D> AggregatePublisher<D> aggregateAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz) {
        return collection.aggregate(clientSession, pipeline, clazz);
    }

    @Override
    public PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline) {
        return Wrappers.toPublisherBuilder(collection.aggregate(pipeline));
    }

    @Override
    public <D> PublisherBuilder<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.aggregate(pipeline, clazz));
    }

    @Override
    public PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toPublisherBuilder(collection.aggregate(clientSession, pipeline));
    }

    @Override
    public <D> PublisherBuilder<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.aggregate(clientSession, pipeline, clazz));
    }

    private <D> AggregatePublisher<D> apply(AggregateOptions options, AggregatePublisher<D> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    @Override
    public PublisherBuilder<Document> aggregate(List<? extends Bson> pipeline, AggregateOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, collection.aggregate(pipeline)));
    }

    @Override
    public <D> PublisherBuilder<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz, AggregateOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, collection.aggregate(pipeline, clazz)));
    }

    @Override
    public PublisherBuilder<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            AggregateOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, collection.aggregate(clientSession, pipeline)));
    }

    @Override
    public <D> PublisherBuilder<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz,
            AggregateOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, collection.aggregate(clientSession, pipeline, clazz)));
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher() {
        return collection.watch();
    }

    @Override
    public <D> ChangeStreamPublisher<D> watchAsPublisher(Class<D> clazz) {
        return collection.watch(clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(List<? extends Bson> pipeline) {
        return collection.watch(pipeline);
    }

    @Override
    public <D> ChangeStreamPublisher<D> watchAsPublisher(List<? extends Bson> pipeline, Class<D> clazz) {
        return collection.watch(pipeline, clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession) {
        return collection.watch(clientSession);
    }

    @Override
    public <D> ChangeStreamPublisher<D> watchAsPublisher(ClientSession clientSession, Class<D> clazz) {
        return collection.watch(clientSession, clazz);
    }

    @Override
    public ChangeStreamPublisher<Document> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline) {
        return collection.watch(clientSession, pipeline);
    }

    @Override
    public <D> ChangeStreamPublisher<D> watchAsPublisher(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz) {
        return collection.watch(clientSession, pipeline, clazz);
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch() {
        return Wrappers.toPublisherBuilder(collection.watch());
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.watch(clazz));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline) {
        return Wrappers.toPublisherBuilder(collection.watch(pipeline));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.watch(pipeline, clazz));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession) {
        return Wrappers.toPublisherBuilder(collection.watch(clientSession));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.watch(clientSession, clazz));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toPublisherBuilder(collection.watch(clientSession, pipeline));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.watch(clientSession, pipeline, clazz));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch()));
    }

    private <D> ChangeStreamPublisher<D> apply(ChangeStreamOptions options, ChangeStreamPublisher<D> watch) {
        if (options == null) {
            return watch;
        }
        return options.apply(watch);
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(Class<D> clazz, ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(clazz)));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(pipeline)));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz,
            ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(pipeline, clazz)));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(clientSession)));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz,
            ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(clientSession, clazz)));
    }

    @Override
    public PublisherBuilder<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(clientSession, pipeline)));
    }

    @Override
    public <D> PublisherBuilder<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz, ChangeStreamOptions options) {
        return Wrappers.toPublisherBuilder(apply(options, collection.watch(clientSession, pipeline, clazz)));
    }

    @Override
    public MapReducePublisher<Document> mapReduceAsPublisher(String mapFunction, String reduceFunction) {
        return collection.mapReduce(mapFunction, reduceFunction);
    }

    @Override
    public <D> MapReducePublisher<D> mapReduceAsPublisher(String mapFunction, String reduceFunction, Class<D> clazz) {
        return collection.mapReduce(mapFunction, reduceFunction, clazz);
    }

    @Override
    public MapReducePublisher<Document> mapReduceAsPublisher(ClientSession clientSession, String mapFunction,
            String reduceFunction) {
        return collection.mapReduce(clientSession, mapFunction, reduceFunction);
    }

    @Override
    public <D> MapReducePublisher<D> mapReduceAsPublisher(ClientSession clientSession, String mapFunction,
            String reduceFunction, Class<D> clazz) {
        return collection.mapReduce(clientSession, mapFunction, reduceFunction, clazz);
    }

    @Override
    public PublisherBuilder<Document> mapReduce(String mapFunction, String reduceFunction) {
        return Wrappers.toPublisherBuilder(mapReduceAsPublisher(mapFunction, reduceFunction));
    }

    @Override
    public <D> PublisherBuilder<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(mapReduceAsPublisher(mapFunction, reduceFunction, clazz));
    }

    @Override
    public PublisherBuilder<Document> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
        return Wrappers.toPublisherBuilder(mapReduceAsPublisher(clientSession, mapFunction, reduceFunction));
    }

    @Override
    public <D> PublisherBuilder<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz) {
        return Wrappers.toPublisherBuilder(mapReduceAsPublisher(clientSession, mapFunction, reduceFunction, clazz));
    }

    @Override
    public PublisherBuilder<Document> mapReduce(String mapFunction, String reduceFunction, MapReduceOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, mapReduceAsPublisher(mapFunction, reduceFunction)));
    }

    private <D> MapReducePublisher<D> apply(MapReduceOptions options, MapReducePublisher<D> mapReduce) {
        if (options == null) {
            return mapReduce;
        }
        return options.apply(mapReduce);
    }

    @Override
    public <D> PublisherBuilder<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz,
            MapReduceOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, mapReduceAsPublisher(mapFunction, reduceFunction, clazz)));
    }

    @Override
    public PublisherBuilder<Document> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            MapReduceOptions options) {
        return ReactiveStreams.fromPublisher(apply(options, mapReduceAsPublisher(clientSession, mapFunction, reduceFunction)));
    }

    @Override
    public <D> PublisherBuilder<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz, MapReduceOptions options) {
        return ReactiveStreams
                .fromPublisher(apply(options, mapReduceAsPublisher(clientSession, mapFunction, reduceFunction, clazz)));
    }

    @Override
    public CompletionStage<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests) {
        return Wrappers.toCompletionStage(collection.bulkWrite(requests));
    }

    @Override
    public CompletionStage<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests,
            BulkWriteOptions options) {
        return Wrappers.toCompletionStage(collection.bulkWrite(requests, options));
    }

    @Override
    public CompletionStage<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests) {
        return Wrappers.toCompletionStage(collection.bulkWrite(clientSession, requests));
    }

    @Override
    public CompletionStage<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests, BulkWriteOptions options) {
        return Wrappers.toCompletionStage(collection.bulkWrite(clientSession, requests, options));
    }

    @Override
    public CompletionStage<Void> insertOne(T t) {
        return Wrappers.toEmptyCompletionStage(collection.insertOne(t));
    }

    @Override
    public CompletionStage<Void> insertOne(T t, InsertOneOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.insertOne(t, options));
    }

    @Override
    public CompletionStage<Void> insertOne(ClientSession clientSession, T t) {
        return Wrappers.toEmptyCompletionStage(collection.insertOne(clientSession, t));
    }

    @Override
    public CompletionStage<Void> insertOne(ClientSession clientSession, T t, InsertOneOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.insertOne(clientSession, t, options));
    }

    @Override
    public CompletionStage<Void> insertMany(List<? extends T> tDocuments) {
        return Wrappers.toEmptyCompletionStage(collection.insertMany(tDocuments));
    }

    @Override
    public CompletionStage<Void> insertMany(List<? extends T> tDocuments, InsertManyOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.insertMany(tDocuments, options));
    }

    @Override
    public CompletionStage<Void> insertMany(ClientSession clientSession, List<? extends T> tDocuments) {
        return Wrappers.toEmptyCompletionStage(collection.insertMany(clientSession, tDocuments));
    }

    @Override
    public CompletionStage<Void> insertMany(ClientSession clientSession, List<? extends T> tDocuments,
            InsertManyOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.insertMany(clientSession, tDocuments, options));
    }

    @Override
    public CompletionStage<DeleteResult> deleteOne(Bson filter) {
        return Wrappers.toCompletionStage(collection.deleteOne(filter));
    }

    @Override
    public CompletionStage<DeleteResult> deleteOne(Bson filter, DeleteOptions options) {
        return Wrappers.toCompletionStage(collection.deleteOne(filter, options));
    }

    @Override
    public CompletionStage<DeleteResult> deleteOne(ClientSession clientSession, Bson filter) {
        return Wrappers.toCompletionStage(collection.deleteOne(clientSession, filter));
    }

    @Override
    public CompletionStage<DeleteResult> deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return Wrappers.toCompletionStage(collection.deleteOne(clientSession, filter, options));
    }

    @Override
    public CompletionStage<DeleteResult> deleteMany(Bson filter) {
        return Wrappers.toCompletionStage(collection.deleteMany(filter));
    }

    @Override
    public CompletionStage<DeleteResult> deleteMany(Bson filter, DeleteOptions options) {
        return Wrappers.toCompletionStage(collection.deleteMany(filter, options));
    }

    @Override
    public CompletionStage<DeleteResult> deleteMany(ClientSession clientSession, Bson filter) {
        return Wrappers.toCompletionStage(collection.deleteMany(clientSession, filter));
    }

    @Override
    public CompletionStage<DeleteResult> deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return Wrappers.toCompletionStage(collection.deleteMany(clientSession, filter, options));
    }

    @Override
    public CompletionStage<UpdateResult> replaceOne(Bson filter, T replacement) {
        return Wrappers.toCompletionStage(collection.replaceOne(filter, replacement));
    }

    @Override
    public CompletionStage<UpdateResult> replaceOne(Bson filter, T replacement, ReplaceOptions options) {
        return Wrappers.toCompletionStage(collection.replaceOne(filter, replacement, options));
    }

    @Override
    public CompletionStage<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement) {
        return Wrappers.toCompletionStage(collection.replaceOne(clientSession, filter, replacement));
    }

    @Override
    public CompletionStage<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement,
            ReplaceOptions options) {
        return Wrappers.toCompletionStage(collection.replaceOne(clientSession, filter, replacement, options));
    }

    @Override
    public CompletionStage<UpdateResult> updateOne(Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.updateOne(filter, update));
    }

    @Override
    public CompletionStage<UpdateResult> updateOne(Bson filter, Bson update, UpdateOptions options) {
        return Wrappers.toCompletionStage(collection.updateOne(filter, update, options));
    }

    @Override
    public CompletionStage<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.updateOne(clientSession, filter, update));
    }

    @Override
    public CompletionStage<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options) {
        return Wrappers.toCompletionStage(collection.updateOne(clientSession, filter, update, options));
    }

    @Override
    public CompletionStage<UpdateResult> updateMany(Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.updateMany(filter, update));
    }

    @Override
    public CompletionStage<UpdateResult> updateMany(Bson filter, Bson update, UpdateOptions options) {
        return Wrappers.toCompletionStage(collection.updateMany(filter, update, options));
    }

    @Override
    public CompletionStage<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.updateMany(clientSession, filter, update));
    }

    @Override
    public CompletionStage<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options) {
        return Wrappers.toCompletionStage(collection.updateMany(clientSession, filter, update, options));
    }

    @Override
    public CompletionStage<T> findOneAndDelete(Bson filter) {
        return Wrappers.toCompletionStage(collection.findOneAndDelete(filter));
    }

    @Override
    public CompletionStage<T> findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndDelete(filter, options));
    }

    @Override
    public CompletionStage<T> findOneAndDelete(ClientSession clientSession, Bson filter) {
        return Wrappers.toCompletionStage(collection.findOneAndDelete(clientSession, filter));
    }

    @Override
    public CompletionStage<T> findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndDelete(clientSession, filter, options));
    }

    @Override
    public CompletionStage<T> findOneAndReplace(Bson filter, T replacement) {
        return Wrappers.toCompletionStage(collection.findOneAndReplace(filter, replacement));
    }

    @Override
    public CompletionStage<T> findOneAndReplace(Bson filter, T replacement, FindOneAndReplaceOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndReplace(filter, replacement, options));
    }

    @Override
    public CompletionStage<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement) {
        return Wrappers.toCompletionStage(collection.findOneAndReplace(clientSession, filter, replacement));
    }

    @Override
    public CompletionStage<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement,
            FindOneAndReplaceOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndReplace(clientSession, filter, replacement, options));
    }

    @Override
    public CompletionStage<T> findOneAndUpdate(Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.findOneAndUpdate(filter, update));
    }

    @Override
    public CompletionStage<T> findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndUpdate(filter, update, options));
    }

    @Override
    public CompletionStage<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toCompletionStage(collection.findOneAndUpdate(clientSession, filter, update));
    }

    @Override
    public CompletionStage<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update,
            FindOneAndUpdateOptions options) {
        return Wrappers.toCompletionStage(collection.findOneAndUpdate(clientSession, filter, update, options));
    }

    @Override
    public CompletionStage<Void> drop() {
        return Wrappers.toEmptyCompletionStage(collection.drop());
    }

    @Override
    public CompletionStage<Void> drop(ClientSession clientSession) {
        return Wrappers.toEmptyCompletionStage(collection.drop(clientSession));
    }

    @Override
    public CompletionStage<String> createIndex(Bson key) {
        return Wrappers.toCompletionStage(collection.createIndex(key));
    }

    @Override
    public CompletionStage<String> createIndex(Bson key, IndexOptions options) {
        return Wrappers.toCompletionStage(collection.createIndex(key, options));
    }

    @Override
    public CompletionStage<String> createIndex(ClientSession clientSession, Bson key) {
        return Wrappers.toCompletionStage(collection.createIndex(clientSession, key));
    }

    @Override
    public CompletionStage<String> createIndex(ClientSession clientSession, Bson key, IndexOptions options) {
        return Wrappers.toCompletionStage(collection.createIndex(clientSession, key, options));
    }

    @Override
    public CompletionStage<List<String>> createIndexes(List<IndexModel> indexes) {
        return Wrappers.toCompletionStageOfList(collection.createIndexes(indexes));
    }

    @Override
    public CompletionStage<List<String>> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return Wrappers.toCompletionStageOfList(collection.createIndexes(indexes, createIndexOptions));
    }

    @Override
    public CompletionStage<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return Wrappers.toCompletionStageOfList(collection.createIndexes(clientSession, indexes));
    }

    @Override
    public CompletionStage<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes,
            CreateIndexOptions createIndexOptions) {
        return Wrappers.toCompletionStageOfList(collection.createIndexes(clientSession, indexes, createIndexOptions));
    }

    @Override
    public ListIndexesPublisher<Document> listIndexesAsPublisher() {
        return collection.listIndexes();
    }

    @Override
    public <D> ListIndexesPublisher<D> listIndexesAsPublisher(Class<D> clazz) {
        return collection.listIndexes(clazz);
    }

    @Override
    public ListIndexesPublisher<Document> listIndexesAsPublisher(ClientSession clientSession) {
        return collection.listIndexes(clientSession);
    }

    @Override
    public <D> ListIndexesPublisher<D> listIndexesAsPublisher(ClientSession clientSession, Class<D> clazz) {
        return collection.listIndexes(clientSession, clazz);
    }

    @Override
    public PublisherBuilder<Document> listIndexes() {
        return Wrappers.toPublisherBuilder(collection.listIndexes());
    }

    @Override
    public <D> PublisherBuilder<D> listIndexes(Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.listIndexes(clazz));
    }

    @Override
    public PublisherBuilder<Document> listIndexes(ClientSession clientSession) {
        return Wrappers.toPublisherBuilder(collection.listIndexes(clientSession));
    }

    @Override
    public <D> PublisherBuilder<D> listIndexes(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toPublisherBuilder(collection.listIndexes(clientSession, clazz));
    }

    @Override
    public CompletionStage<Void> dropIndex(String indexName) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(indexName));
    }

    @Override
    public CompletionStage<Void> dropIndex(Bson keys) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(keys));
    }

    @Override
    public CompletionStage<Void> dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(indexName, dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(keys, dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> dropIndex(ClientSession clientSession, String indexName) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(clientSession, indexName));
    }

    @Override
    public CompletionStage<Void> dropIndex(ClientSession clientSession, Bson keys) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(clientSession, keys));
    }

    @Override
    public CompletionStage<Void> dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(clientSession, indexName, dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndex(clientSession, keys, dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> dropIndexes() {
        return Wrappers.toEmptyCompletionStage(collection.dropIndexes());
    }

    @Override
    public CompletionStage<Void> dropIndexes(DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndexes(dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> dropIndexes(ClientSession clientSession) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndexes(clientSession));
    }

    @Override
    public CompletionStage<Void> dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
        return Wrappers.toEmptyCompletionStage(collection.dropIndexes(clientSession, dropIndexOptions));
    }

    @Override
    public CompletionStage<Void> renameCollection(MongoNamespace newCollectionNamespace) {
        return Wrappers.toEmptyCompletionStage(collection.renameCollection(newCollectionNamespace));
    }

    @Override
    public CompletionStage<Void> renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.renameCollection(newCollectionNamespace, options));
    }

    @Override
    public CompletionStage<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
        return Wrappers.toEmptyCompletionStage(collection.renameCollection(clientSession, newCollectionNamespace));
    }

    @Override
    public CompletionStage<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace,
            RenameCollectionOptions options) {
        return Wrappers.toEmptyCompletionStage(collection.renameCollection(clientSession, newCollectionNamespace, options));
    }
}
