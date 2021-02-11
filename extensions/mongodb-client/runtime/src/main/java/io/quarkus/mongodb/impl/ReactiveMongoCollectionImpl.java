package io.quarkus.mongodb.impl;

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
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.DistinctPublisher;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MapReducePublisher;
import com.mongodb.reactivestreams.client.MongoCollection;

import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.DistinctOptions;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.MapReduceOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

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
    public Uni<Long> estimatedDocumentCount() {
        return Wrappers.toUni(collection.estimatedDocumentCount());
    }

    @Override
    public Uni<Long> estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return Wrappers.toUni(collection.estimatedDocumentCount(options));
    }

    @Override
    public Uni<Long> countDocuments() {
        return Wrappers.toUni(collection.countDocuments());
    }

    @Override
    public Uni<Long> countDocuments(Bson filter) {
        return Wrappers.toUni(collection.countDocuments(filter));
    }

    @Override
    public Uni<Long> countDocuments(Bson filter, CountOptions options) {
        return Wrappers.toUni(collection.countDocuments(filter, options));
    }

    @Override
    public Uni<Long> countDocuments(ClientSession clientSession) {
        return Wrappers.toUni(collection.countDocuments(clientSession));
    }

    @Override
    public Uni<Long> countDocuments(ClientSession clientSession, Bson filter) {
        return Wrappers.toUni(collection.countDocuments(clientSession, filter));
    }

    @Override
    public Uni<Long> countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return Wrappers.toUni(collection.countDocuments(clientSession, filter, options));
    }

    @Override
    public <D> Multi<D> distinct(String fieldName, Class<D> clazz) {
        return Wrappers.toMulti(collection.distinct(fieldName, clazz));
    }

    @Override
    public <D> Multi<D> distinct(String fieldName, Bson filter, Class<D> clazz) {
        return Wrappers.toMulti(collection.distinct(fieldName, filter, clazz));
    }

    @Override
    public <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz) {
        return Wrappers.toMulti(collection.distinct(clientSession, fieldName, clazz));
    }

    @Override
    public <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz) {
        return Wrappers.toMulti(collection.distinct(clientSession, fieldName, filter, clazz));
    }

    private <D> DistinctPublisher<D> apply(DistinctOptions options, DistinctPublisher<D> stream) {
        if (options == null) {
            return stream;
        }
        return options.apply(stream);
    }

    @Override
    public <D> Multi<D> distinct(String fieldName, Class<D> clazz, DistinctOptions options) {
        return Wrappers.toMulti(apply(options, collection.distinct(fieldName, clazz)));
    }

    @Override
    public <D> Multi<D> distinct(String fieldName, Bson filter, Class<D> clazz, DistinctOptions options) {
        return Wrappers.toMulti(apply(options, collection.distinct(fieldName, filter, clazz)));
    }

    @Override
    public <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Class<D> clazz,
            DistinctOptions options) {
        return Wrappers.toMulti(apply(options, collection.distinct(clientSession, fieldName, clazz)));
    }

    @Override
    public <D> Multi<D> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<D> clazz,
            DistinctOptions options) {
        return Wrappers.toMulti(apply(options, collection.distinct(clientSession, fieldName, filter, clazz)));
    }

    @Override
    public Multi<T> find() {
        return Wrappers.toMulti(collection.find());
    }

    @Override
    public <D> Multi<D> find(Class<D> clazz) {
        return Wrappers.toMulti(collection.find(clazz));
    }

    @Override
    public Multi<T> find(Bson filter) {
        return Wrappers.toMulti(collection.find(filter));
    }

    @Override
    public <D> Multi<D> find(Bson filter, Class<D> clazz) {
        return Wrappers.toMulti(collection.find(filter, clazz));
    }

    @Override
    public Multi<T> find(ClientSession clientSession) {
        return Wrappers.toMulti(collection.find(clientSession));
    }

    @Override
    public <D> Multi<D> find(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toMulti(collection.find(clientSession, clazz));
    }

    @Override
    public Multi<T> find(ClientSession clientSession, Bson filter) {
        return Wrappers.toMulti(collection.find(clientSession, filter));
    }

    @Override
    public <D> Multi<D> find(ClientSession clientSession, Bson filter, Class<D> clazz) {
        return Wrappers.toMulti(collection.find(clientSession, filter, clazz));
    }

    private <D> FindPublisher<D> apply(FindOptions options, FindPublisher<D> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    @Override
    public Multi<T> find(FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find()));
    }

    @Override
    public <D> Multi<D> find(Class<D> clazz, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(clazz)));
    }

    @Override
    public Multi<T> find(Bson filter, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(filter)));
    }

    @Override
    public <D> Multi<D> find(Bson filter, Class<D> clazz, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(filter, clazz)));
    }

    @Override
    public Multi<T> find(ClientSession clientSession, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(clientSession)));
    }

    @Override
    public <D> Multi<D> find(ClientSession clientSession, Class<D> clazz, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(clientSession, clazz)));
    }

    @Override
    public Multi<T> find(ClientSession clientSession, Bson filter, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(clientSession, filter)));
    }

    @Override
    public <D> Multi<D> find(ClientSession clientSession, Bson filter, Class<D> clazz, FindOptions options) {
        return Wrappers.toMulti(apply(options, collection.find(clientSession, filter, clazz)));
    }

    @Override
    public Multi<T> aggregate(List<? extends Bson> pipeline) {
        return Wrappers.toMulti(collection.aggregate(pipeline));
    }

    @Override
    public <D> Multi<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toMulti(collection.aggregate(pipeline, clazz));
    }

    @Override
    public Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toMulti(collection.aggregate(clientSession, pipeline));
    }

    @Override
    public <D> Multi<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toMulti(collection.aggregate(clientSession, pipeline, clazz));
    }

    private <D> AggregatePublisher<D> apply(AggregateOptions options, AggregatePublisher<D> publisher) {
        if (options == null) {
            return publisher;
        }
        return options.apply(publisher);
    }

    @Override
    public Multi<T> aggregate(List<? extends Bson> pipeline, AggregateOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.aggregate(pipeline)));
    }

    @Override
    public <D> Multi<D> aggregate(List<? extends Bson> pipeline, Class<D> clazz, AggregateOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.aggregate(pipeline, clazz)));
    }

    @Override
    public Multi<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            AggregateOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.aggregate(clientSession, pipeline)));
    }

    @Override
    public <D> Multi<D> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<D> clazz,
            AggregateOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.aggregate(clientSession, pipeline, clazz)));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch() {
        return Wrappers.toMulti(collection.watch());
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(Class<D> clazz) {
        return Wrappers.toMulti(collection.watch(clazz));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline) {
        return Wrappers.toMulti(collection.watch(pipeline));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz) {
        return Wrappers.toMulti(collection.watch(pipeline, clazz));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession) {
        return Wrappers.toMulti(collection.watch(clientSession));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toMulti(collection.watch(clientSession, clazz));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return Wrappers.toMulti(collection.watch(clientSession, pipeline));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz) {
        return Wrappers.toMulti(collection.watch(clientSession, pipeline, clazz));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch()));
    }

    private <D> ChangeStreamPublisher<D> apply(ChangeStreamOptions options, ChangeStreamPublisher<D> watch) {
        if (options == null) {
            return watch;
        }
        return options.apply(watch);
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(Class<D> clazz, ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(clazz)));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(List<? extends Bson> pipeline, ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(pipeline)));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(List<? extends Bson> pipeline, Class<D> clazz,
            ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(pipeline, clazz)));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(clientSession)));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, Class<D> clazz,
            ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(clientSession, clazz)));
    }

    @Override
    public Multi<ChangeStreamDocument<Document>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(clientSession, pipeline)));
    }

    @Override
    public <D> Multi<ChangeStreamDocument<D>> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<D> clazz, ChangeStreamOptions options) {
        return Wrappers.toMulti(apply(options, collection.watch(clientSession, pipeline, clazz)));
    }

    @Override
    public Multi<T> mapReduce(String mapFunction, String reduceFunction) {
        return Wrappers.toMulti(collection.mapReduce(mapFunction, reduceFunction));
    }

    @Override
    public <D> Multi<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz) {
        return Wrappers.toMulti(collection.mapReduce(mapFunction, reduceFunction, clazz));
    }

    @Override
    public Multi<T> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
        return Wrappers.toMulti(collection.mapReduce(clientSession, mapFunction, reduceFunction));
    }

    @Override
    public <D> Multi<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz) {
        return Wrappers.toMulti(collection.mapReduce(clientSession, mapFunction, reduceFunction, clazz));
    }

    @Override
    public Multi<T> mapReduce(String mapFunction, String reduceFunction, MapReduceOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.mapReduce(mapFunction, reduceFunction)));
    }

    private <D> MapReducePublisher<D> apply(MapReduceOptions options, MapReducePublisher<D> mapReduce) {
        if (options == null) {
            return mapReduce;
        }
        return options.apply(mapReduce);
    }

    @Override
    public <D> Multi<D> mapReduce(String mapFunction, String reduceFunction, Class<D> clazz,
            MapReduceOptions options) {
        return Multi.createFrom().publisher(apply(options, collection.mapReduce(mapFunction, reduceFunction, clazz)));
    }

    @Override
    public Multi<T> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            MapReduceOptions options) {
        return Multi.createFrom()
                .publisher(apply(options, collection.mapReduce(clientSession, mapFunction, reduceFunction)));
    }

    @Override
    public <D> Multi<D> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction,
            Class<D> clazz, MapReduceOptions options) {
        return Multi.createFrom()
                .publisher(apply(options, collection.mapReduce(clientSession, mapFunction, reduceFunction, clazz)));
    }

    @Override
    public Uni<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests) {
        return Wrappers.toUni(collection.bulkWrite(requests));
    }

    @Override
    public Uni<BulkWriteResult> bulkWrite(List<? extends WriteModel<? extends T>> requests,
            BulkWriteOptions options) {
        return Wrappers.toUni(collection.bulkWrite(requests, options));
    }

    @Override
    public Uni<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests) {
        return Wrappers.toUni(collection.bulkWrite(clientSession, requests));
    }

    @Override
    public Uni<BulkWriteResult> bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends T>> requests, BulkWriteOptions options) {
        return Wrappers.toUni(collection.bulkWrite(clientSession, requests, options));
    }

    @Override
    public Uni<InsertOneResult> insertOne(T t) {
        return Wrappers.toUni(collection.insertOne(t));
    }

    @Override
    public Uni<InsertOneResult> insertOne(T t, InsertOneOptions options) {
        return Wrappers.toUni(collection.insertOne(t, options));
    }

    @Override
    public Uni<InsertOneResult> insertOne(ClientSession clientSession, T t) {
        return Wrappers.toUni(collection.insertOne(clientSession, t));
    }

    @Override
    public Uni<InsertOneResult> insertOne(ClientSession clientSession, T t, InsertOneOptions options) {
        return Wrappers.toUni(collection.insertOne(clientSession, t, options));
    }

    @Override
    public Uni<InsertManyResult> insertMany(List<? extends T> tDocuments) {
        return Wrappers.toUni(collection.insertMany(tDocuments));
    }

    @Override
    public Uni<InsertManyResult> insertMany(List<? extends T> tDocuments, InsertManyOptions options) {
        return Wrappers.toUni(collection.insertMany(tDocuments, options));
    }

    @Override
    public Uni<InsertManyResult> insertMany(ClientSession clientSession, List<? extends T> tDocuments) {
        return Wrappers.toUni(collection.insertMany(clientSession, tDocuments));
    }

    @Override
    public Uni<InsertManyResult> insertMany(ClientSession clientSession, List<? extends T> tDocuments,
            InsertManyOptions options) {
        return Wrappers.toUni(collection.insertMany(clientSession, tDocuments, options));
    }

    @Override
    public Uni<DeleteResult> deleteOne(Bson filter) {
        return Wrappers.toUni(collection.deleteOne(filter));
    }

    @Override
    public Uni<DeleteResult> deleteOne(Bson filter, DeleteOptions options) {
        return Wrappers.toUni(collection.deleteOne(filter, options));
    }

    @Override
    public Uni<DeleteResult> deleteOne(ClientSession clientSession, Bson filter) {
        return Wrappers.toUni(collection.deleteOne(clientSession, filter));
    }

    @Override
    public Uni<DeleteResult> deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return Wrappers.toUni(collection.deleteOne(clientSession, filter, options));
    }

    @Override
    public Uni<DeleteResult> deleteMany(Bson filter) {
        return Wrappers.toUni(collection.deleteMany(filter));
    }

    @Override
    public Uni<DeleteResult> deleteMany(Bson filter, DeleteOptions options) {
        return Wrappers.toUni(collection.deleteMany(filter, options));
    }

    @Override
    public Uni<DeleteResult> deleteMany(ClientSession clientSession, Bson filter) {
        return Wrappers.toUni(collection.deleteMany(clientSession, filter));
    }

    @Override
    public Uni<DeleteResult> deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return Wrappers.toUni(collection.deleteMany(clientSession, filter, options));
    }

    @Override
    public Uni<UpdateResult> replaceOne(Bson filter, T replacement) {
        return Wrappers.toUni(collection.replaceOne(filter, replacement));
    }

    @Override
    public Uni<UpdateResult> replaceOne(Bson filter, T replacement, ReplaceOptions options) {
        return Wrappers.toUni(collection.replaceOne(filter, replacement, options));
    }

    @Override
    public Uni<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement) {
        return Wrappers.toUni(collection.replaceOne(clientSession, filter, replacement));
    }

    @Override
    public Uni<UpdateResult> replaceOne(ClientSession clientSession, Bson filter, T replacement,
            ReplaceOptions options) {
        return Wrappers.toUni(collection.replaceOne(clientSession, filter, replacement, options));
    }

    @Override
    public Uni<UpdateResult> updateOne(Bson filter, Bson update) {
        return Wrappers.toUni(collection.updateOne(filter, update));
    }

    @Override
    public Uni<UpdateResult> updateOne(Bson filter, Bson update, UpdateOptions options) {
        return Wrappers.toUni(collection.updateOne(filter, update, options));
    }

    @Override
    public Uni<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toUni(collection.updateOne(clientSession, filter, update));
    }

    @Override
    public Uni<UpdateResult> updateOne(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options) {
        return Wrappers.toUni(collection.updateOne(clientSession, filter, update, options));
    }

    @Override
    public Uni<UpdateResult> updateMany(Bson filter, Bson update) {
        return Wrappers.toUni(collection.updateMany(filter, update));
    }

    @Override
    public Uni<UpdateResult> updateMany(Bson filter, Bson update, UpdateOptions options) {
        return Wrappers.toUni(collection.updateMany(filter, update, options));
    }

    @Override
    public Uni<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toUni(collection.updateMany(clientSession, filter, update));
    }

    @Override
    public Uni<UpdateResult> updateMany(ClientSession clientSession, Bson filter, Bson update,
            UpdateOptions options) {
        return Wrappers.toUni(collection.updateMany(clientSession, filter, update, options));
    }

    @Override
    public Uni<T> findOneAndDelete(Bson filter) {
        return Wrappers.toUni(collection.findOneAndDelete(filter));
    }

    @Override
    public Uni<T> findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return Wrappers.toUni(collection.findOneAndDelete(filter, options));
    }

    @Override
    public Uni<T> findOneAndDelete(ClientSession clientSession, Bson filter) {
        return Wrappers.toUni(collection.findOneAndDelete(clientSession, filter));
    }

    @Override
    public Uni<T> findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return Wrappers.toUni(collection.findOneAndDelete(clientSession, filter, options));
    }

    @Override
    public Uni<T> findOneAndReplace(Bson filter, T replacement) {
        return Wrappers.toUni(collection.findOneAndReplace(filter, replacement));
    }

    @Override
    public Uni<T> findOneAndReplace(Bson filter, T replacement, FindOneAndReplaceOptions options) {
        return Wrappers.toUni(collection.findOneAndReplace(filter, replacement, options));
    }

    @Override
    public Uni<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement) {
        return Wrappers.toUni(collection.findOneAndReplace(clientSession, filter, replacement));
    }

    @Override
    public Uni<T> findOneAndReplace(ClientSession clientSession, Bson filter, T replacement,
            FindOneAndReplaceOptions options) {
        return Wrappers.toUni(collection.findOneAndReplace(clientSession, filter, replacement, options));
    }

    @Override
    public Uni<T> findOneAndUpdate(Bson filter, Bson update) {
        return Wrappers.toUni(collection.findOneAndUpdate(filter, update));
    }

    @Override
    public Uni<T> findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return Wrappers.toUni(collection.findOneAndUpdate(filter, update, options));
    }

    @Override
    public Uni<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return Wrappers.toUni(collection.findOneAndUpdate(clientSession, filter, update));
    }

    @Override
    public Uni<T> findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update,
            FindOneAndUpdateOptions options) {
        return Wrappers.toUni(collection.findOneAndUpdate(clientSession, filter, update, options));
    }

    @Override
    public Uni<Void> drop() {
        return Wrappers.toUni(collection.drop());
    }

    @Override
    public Uni<Void> drop(ClientSession clientSession) {
        return Wrappers.toUni(collection.drop(clientSession));
    }

    @Override
    public Uni<String> createIndex(Bson key) {
        return Wrappers.toUni(collection.createIndex(key));
    }

    @Override
    public Uni<String> createIndex(Bson key, IndexOptions options) {
        return Wrappers.toUni(collection.createIndex(key, options));
    }

    @Override
    public Uni<String> createIndex(ClientSession clientSession, Bson key) {
        return Wrappers.toUni(collection.createIndex(clientSession, key));
    }

    @Override
    public Uni<String> createIndex(ClientSession clientSession, Bson key, IndexOptions options) {
        return Wrappers.toUni(collection.createIndex(clientSession, key, options));
    }

    @Override
    public Uni<List<String>> createIndexes(List<IndexModel> indexes) {
        return Wrappers.toUniOfList(collection.createIndexes(indexes));
    }

    @Override
    public Uni<List<String>> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return Wrappers.toUniOfList(collection.createIndexes(indexes, createIndexOptions));
    }

    @Override
    public Uni<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return Wrappers.toUniOfList(collection.createIndexes(clientSession, indexes));
    }

    @Override
    public Uni<List<String>> createIndexes(ClientSession clientSession, List<IndexModel> indexes,
            CreateIndexOptions createIndexOptions) {
        return Wrappers.toUniOfList(collection.createIndexes(clientSession, indexes, createIndexOptions));
    }

    @Override
    public Multi<Document> listIndexes() {
        return Wrappers.toMulti(collection.listIndexes());
    }

    @Override
    public <D> Multi<D> listIndexes(Class<D> clazz) {
        return Wrappers.toMulti(collection.listIndexes(clazz));
    }

    @Override
    public Multi<Document> listIndexes(ClientSession clientSession) {
        return Wrappers.toMulti(collection.listIndexes(clientSession));
    }

    @Override
    public <D> Multi<D> listIndexes(ClientSession clientSession, Class<D> clazz) {
        return Wrappers.toMulti(collection.listIndexes(clientSession, clazz));
    }

    @Override
    public Uni<Void> dropIndex(String indexName) {
        return Wrappers.toUni(collection.dropIndex(indexName));
    }

    @Override
    public Uni<Void> dropIndex(Bson keys) {
        return Wrappers.toUni(collection.dropIndex(keys));
    }

    @Override
    public Uni<Void> dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndex(indexName, dropIndexOptions));
    }

    @Override
    public Uni<Void> dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndex(keys, dropIndexOptions));
    }

    @Override
    public Uni<Void> dropIndex(ClientSession clientSession, String indexName) {
        return Wrappers.toUni(collection.dropIndex(clientSession, indexName));
    }

    @Override
    public Uni<Void> dropIndex(ClientSession clientSession, Bson keys) {
        return Wrappers.toUni(collection.dropIndex(clientSession, keys));
    }

    @Override
    public Uni<Void> dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndex(clientSession, indexName, dropIndexOptions));
    }

    @Override
    public Uni<Void> dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndex(clientSession, keys, dropIndexOptions));
    }

    @Override
    public Uni<Void> dropIndexes() {
        return Wrappers.toUni(collection.dropIndexes());
    }

    @Override
    public Uni<Void> dropIndexes(DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndexes(dropIndexOptions));
    }

    @Override
    public Uni<Void> dropIndexes(ClientSession clientSession) {
        return Wrappers.toUni(collection.dropIndexes(clientSession));
    }

    @Override
    public Uni<Void> dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
        return Wrappers.toUni(collection.dropIndexes(clientSession, dropIndexOptions));
    }

    @Override
    public Uni<Void> renameCollection(MongoNamespace newCollectionNamespace) {
        return Wrappers.toUni(collection.renameCollection(newCollectionNamespace));
    }

    @Override
    public Uni<Void> renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions options) {
        return Wrappers.toUni(collection.renameCollection(newCollectionNamespace, options));
    }

    @Override
    public Uni<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
        return Wrappers.toUni(collection.renameCollection(clientSession, newCollectionNamespace));
    }

    @Override
    public Uni<Void> renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace,
            RenameCollectionOptions options) {
        return Wrappers.toUni(collection.renameCollection(clientSession, newCollectionNamespace, options));
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return collection.getCodecRegistry();
    }

    @Override
    public <NewTDocument> ReactiveMongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
        return new ReactiveMongoCollectionImpl<>(this.collection.withDocumentClass(clazz));
    }

    @Override
    public ReactiveMongoCollectionImpl<T> withReadPreference(ReadPreference readPreference) {
        return new ReactiveMongoCollectionImpl<>(this.collection.withReadPreference(readPreference));
    }
}
