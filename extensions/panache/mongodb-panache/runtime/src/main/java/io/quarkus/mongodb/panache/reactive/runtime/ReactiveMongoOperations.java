package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.Bean;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.runtime.MongoOperations;
import io.quarkus.mongodb.panache.transaction.interceptor.ReactiveMongoTransaction;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactiveMongoOperations {
    private static final Logger LOGGER = Logger.getLogger(ReactiveMongoOperations.class);
    public static final String ID = "_id";
    public static final String MONGODB_DATABASE = "quarkus.mongodb.database";

    private static volatile String defaultDatabaseName;

    //
    // Instance methods

    public static Uni<Void> persist(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return persist(collection, entity);
    }

    public static Uni<Void> persist(Iterable<?> entities) {
        return Uni.createFrom().deferred(() -> {
            // not all iterables are re-traversal, so we traverse it once for copying inside a list
            List<Object> objects = new ArrayList<>();
            for (Object entity : entities) {
                objects.add(entity);
            }

            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return persist(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> persist(Object firstEntity, Object... entities) {
        ReactiveMongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            return persist(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            return persist(collection, entityList);
        }
    }

    public static Uni<Void> persist(Stream<?> entities) {
        return Uni.createFrom().deferred(() -> {
            List<Object> objects = entities.collect(Collectors.toList());
            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return persist(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> update(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return update(collection, entity);
    }

    public static Uni<Void> update(Iterable<?> entities) {
        return Uni.createFrom().deferred(() -> {
            // not all iterables are re-traversal, so we traverse it once for copying inside a list
            List<Object> objects = new ArrayList<>();
            for (Object entity : entities) {
                objects.add(entity);
            }

            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return update(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> update(Object firstEntity, Object... entities) {
        ReactiveMongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            return update(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            return update(collection, entityList);
        }
    }

    public static Uni<Void> update(Stream<?> entities) {
        return Uni.createFrom().deferred(() -> {
            List<Object> objects = entities.collect(Collectors.toList());
            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return update(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> persistOrUpdate(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return persistOrUpdate(collection, entity);
    }

    public static Uni<Void> persistOrUpdate(Iterable<?> entities) {
        return Uni.createFrom().deferred(() -> {
            // not all iterables are re-traversal, so we traverse it once for copying inside a list
            List<Object> objects = new ArrayList<>();
            for (Object entity : entities) {
                objects.add(entity);
            }

            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return persistOrUpdate(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> persistOrUpdate(Object firstEntity, Object... entities) {
        ReactiveMongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            return persistOrUpdate(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            return persistOrUpdate(collection, entityList);
        }
    }

    public static Uni<Void> persistOrUpdate(Stream<?> entities) {
        return Uni.createFrom().deferred(() -> {
            List<Object> objects = entities.collect(Collectors.toList());
            if (objects.size() > 0) {
                // get the first entity to be able to retrieve the collection with it
                Object firstEntity = objects.get(0);
                ReactiveMongoCollection collection = mongoCollection(firstEntity);
                return persistOrUpdate(collection, objects);
            }
            return nullUni();
        });
    }

    public static Uni<Void> delete(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        BsonDocument document = getBsonDocument(collection, entity);
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        ClientSession session = getSession();
        Uni<DeleteResult> result = session == null ? collection.deleteOne(query) : collection.deleteOne(session, query);
        return result.onItem().ignore().andContinueWithNull();
    }

    public static ReactiveMongoCollection mongoCollection(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        ReactiveMongoDatabase database = mongoDatabase(mongoEntity);
        if (mongoEntity != null && !mongoEntity.collection().isEmpty()) {
            return database.getCollection(mongoEntity.collection(), entityClass);
        }
        return database.getCollection(entityClass.getSimpleName(), entityClass);
    }

    public static ReactiveMongoDatabase mongoDatabase(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        return mongoDatabase(mongoEntity);
    }

    //
    // Private stuff

    public static Uni<Void> nullUni() {
        return Uni.createFrom().item((Void) null);
    }

    private static Uni<Void> persist(ReactiveMongoCollection collection, Object entity) {
        ClientSession session = getSession();
        Uni<InsertOneResult> result = session == null ? collection.insertOne(entity) : collection.insertOne(session, entity);
        return result.onItem().ignore().andContinueWithNull();
    }

    private static Uni<Void> persist(ReactiveMongoCollection collection, List<Object> entities) {
        ClientSession session = getSession();
        Uni<InsertManyResult> results = session == null ? collection.insertMany(entities)
                : collection.insertMany(session, entities);
        return results.onItem().ignore().andContinueWithNull();
    }

    private static Uni<Void> update(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        ClientSession session = getSession();
        Uni<UpdateResult> result = session == null ? collection.replaceOne(query, entity)
                : collection.replaceOne(session, query, entity);
        return result.onItem().ignore().andContinueWithNull();
    }

    private static Uni<Void> update(ReactiveMongoCollection collection, List<Object> entities) {
        List<Uni<Void>> unis = entities.stream().map(entity -> update(collection, entity)).collect(Collectors.toList());
        return Uni.combine().all().unis(unis).combinedWith(u -> null);
    }

    private static Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            return persist(entity);
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            ReplaceOptions options = new ReplaceOptions().upsert(true);
            ClientSession session = getSession();
            Uni<UpdateResult> result = session == null ? collection.replaceOne(query, entity, options)
                    : collection.replaceOne(session, query, entity, options);
            return result.onItem().ignore().andContinueWithNull();
        }
    }

    private static Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, List<Object> entities) {
        //this will be an ordered bulk: it's less performant than a unordered one but will fail at the first failed write
        List<WriteModel> bulk = new ArrayList<>();
        for (Object entity : entities) {
            //we transform the entity as a document first
            BsonDocument document = getBsonDocument(collection, entity);

            //then we get its id field and create a new Document with only this one that will be our replace query
            BsonValue id = document.get(ID);
            if (id == null) {
                //insert with autogenerated ID
                bulk.add(new InsertOneModel(entity));
            } else {
                //insert with user provided ID or update
                BsonDocument query = new BsonDocument().append(ID, id);
                bulk.add(new ReplaceOneModel(query, entity,
                        new ReplaceOptions().upsert(true)));
            }
        }

        ClientSession session = getSession();
        Uni<BulkWriteResult> results = session == null ? collection.bulkWrite(bulk) : collection.bulkWrite(session, bulk);
        return results.onItem().ignore().andContinueWithNull();
    }

    private static BsonDocument getBsonDocument(ReactiveMongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }

    static ClientSession getSession() {
        InstanceHandle<TransactionSynchronizationRegistry> instance = Arc.container()
                .instance(TransactionSynchronizationRegistry.class);
        if (instance.isAvailable()) {
            TransactionSynchronizationRegistry registry = instance.get();
            if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
                ReactiveMongoTransaction mongoTransaction = (ReactiveMongoTransaction) registry
                        .getResource(registry.getTransactionKey());
                return mongoTransaction.getClientSession();
            }
        }
        return null;
    }

    private static ReactiveMongoCollection mongoCollection(Object entity) {
        Class<?> entityClass = entity.getClass();
        return mongoCollection(entityClass);
    }

    private static ReactiveMongoDatabase mongoDatabase(MongoEntity entity) {
        ReactiveMongoClient mongoClient = mongoClient(entity);
        if (entity != null && !entity.database().isEmpty()) {
            return mongoClient.getDatabase(entity.database());
        }
        String databaseName = getDefaultDatabaseName();
        return mongoClient.getDatabase(databaseName);
    }

    private static String getDefaultDatabaseName() {
        if (defaultDatabaseName == null) {
            synchronized (MongoOperations.class) {
                if (defaultDatabaseName == null) {
                    defaultDatabaseName = ConfigProvider.getConfig()
                            .getValue(MONGODB_DATABASE, String.class);
                }
            }
        }
        return defaultDatabaseName;
    }

    private static ReactiveMongoClient mongoClient(MongoEntity entity) {
        if (entity != null && !entity.clientName().isEmpty()) {
            Set<Bean<?>> beans = Arc.container().beanManager().getBeans(ReactiveMongoClient.class);
            for (Bean<?> bean : beans) {
                if (bean.getName() != null) {
                    return (ReactiveMongoClient) Arc.container().instance(entity.clientName()).get();
                }
            }
        }
        return Arc.container().instance(ReactiveMongoClient.class).get();
    }

    //
    // Queries

    public static Uni<Object> findById(Class<?> entityClass, Object id) {
        Uni<Optional> optionalEntity = findByIdOptional(entityClass, id);

        return optionalEntity.onItem().apply(optional -> optional.orElse(null));
    }

    public static Uni<Optional> findByIdOptional(Class<?> entityClass, Object id) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document query = new Document(ID, id);
        ClientSession session = getSession();
        Multi results = session == null ? collection.find(query) : collection.find(session, query);
        return results.collectItems().first().onItem().apply(Optional::ofNullable);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, docQuery, docSort);
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1</code> for PanacheQL one.
     */
    static String bindFilter(Class<?> clazz, String query, Object[] params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     */
    static String bindFilter(Class<?> clazz, String query, Map<String, Object> params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1 and lastname = ?2</code> for PanacheQL one.
     * As update document needs a <code>$set</code> operator we add it if needed.
     */
    static String bindUpdate(Class<?> clazz, String query, Object[] params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!bindUpdate.contains("$set")) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     * As update document needs a <code>$set</code> operator we add it if needed.
     */
    static String bindUpdate(Class<?> clazz, String query, Map<String, Object> params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!bindUpdate.contains("$set")) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    static String bindQuery(Class<?> clazz, String query, Object[] params) {
        String bindQuery = null;

        //determine the type of the query
        if (query.charAt(0) == '{') {
            //this is a native query
            bindQuery = NativeQueryBinder.bindQuery(query, params);
        } else {
            //this is a PanacheQL query
            bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        }

        return bindQuery;
    }

    static String bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
        String bindQuery = null;

        //determine the type of the query
        if (query.charAt(0) == '{') {
            //this is a native query
            bindQuery = NativeQueryBinder.bindQuery(query, params);
        } else {
            //this is a PanacheQL query
            bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        }

        return bindQuery;
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, docQuery, docSort);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params.map());
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> find(Class<?> entityClass, Document query, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return new ReactivePanacheQueryImpl(collection, query, sortDoc);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, Document query, Document sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, query, sort);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, Document query) {
        return find(entityClass, query, (Document) null);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Object... params) {
        return (Uni) find(entityClass, query, params).list();
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return (Uni) find(entityClass, query, sort, params).list();
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return (Uni) find(entityClass, query, params).list();
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return (Uni) find(entityClass, query, sort, params).list();
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Parameters params) {
        return (Uni) find(entityClass, query, params).list();
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return (Uni) find(entityClass, query, sort, params).list();
    }

    //specific Mongo query
    public static Uni<List<?>> list(Class<?> entityClass, Document query) {
        return (Uni) find(entityClass, query).list();
    }

    //specific Mongo query
    public static Uni<List<?>> list(Class<?> entityClass, Document query, Document sort) {
        return (Uni) find(entityClass, query, sort).list();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, params).stream();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, params).stream();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params).stream();
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params).stream();
    }

    //specific Mongo query
    public static Multi<?> stream(Class<?> entityClass, Document query) {
        return find(entityClass, query).stream();
    }

    //specific Mongo query
    public static Multi<?> stream(Class<?> entityClass, Document query, Document sort) {
        return find(entityClass, query, sort).stream();
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> findAll(Class<?> entityClass) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, null, null);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return new ReactivePanacheQueryImpl(collection, null, sortDoc);
    }

    private static Document sortToDocument(Sort sort) {
        if (sort == null) {
            return null;
        }

        Document sortDoc = new Document();
        for (Sort.Column col : sort.getColumns()) {
            sortDoc.append(col.getName(), col.getDirection() == Sort.Direction.Ascending ? 1 : -1);
        }
        return sortDoc;
    }

    public static Uni<List<?>> listAll(Class<?> entityClass) {
        return (Uni) findAll(entityClass).list();
    }

    public static Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return (Uni) findAll(entityClass, sort).list();
    }

    public static Multi<?> streamAll(Class<?> entityClass) {
        return findAll(entityClass).stream();
    }

    public static Multi<?> streamAll(Class<?> entityClass, Sort sort) {
        return findAll(entityClass, sort).stream();
    }

    public static Uni<Long> count(Class<?> entityClass) {
        return executeCount(entityClass, new BsonDocument());
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeCount(entityClass, docQuery);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeCount(entityClass, docQuery);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public static Uni<Long> count(Class<?> entityClass, Document query) {
        return executeCount(entityClass, query);
    }

    private static Uni<Long> executeCount(Class<?> entityClass, Bson query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        ClientSession session = getSession();
        if (session == null) {
            return collection.countDocuments(query);
        } else {
            return collection.countDocuments(session, query);
        }
    }

    public static Uni<Long> deleteAll(Class<?> entityClass) {
        return executeDelete(entityClass, new BsonDocument());
    }

    public static Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        ClientSession session = getSession();
        Uni<DeleteResult> result = session == null ? collection.deleteOne(query) : collection.deleteOne(session, query);
        return result.map(results -> results.getDeletedCount() == 1);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeDelete(entityClass, docQuery);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeDelete(entityClass, docQuery);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public static Uni<Long> delete(Class<?> entityClass, Document query) {
        return executeDelete(entityClass, query);
    }

    public static Uni<Long> executeDelete(Class<?> entityClass, Bson query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        ClientSession session = getSession();
        Uni<DeleteResult> result = session == null ? collection.deleteMany(query) : collection.deleteMany(session, query);
        return result.map(deleteResult -> deleteResult.getDeletedCount());
    }

    public static ReactivePanacheUpdate update(Class<?> entityClass, String update, Map<String, Object> params) {
        return executeUpdate(entityClass, update, params);
    }

    public static ReactivePanacheUpdate update(Class<?> entityClass, String update, Parameters params) {
        return update(entityClass, update, params.map());
    }

    public static ReactivePanacheUpdate update(Class<?> entityClass, String update, Object... params) {
        return executeUpdate(entityClass, update, params);
    }

    private static ReactivePanacheUpdate executeUpdate(Class<?> entityClass, String update, Object... params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        BsonDocument docUpdate = BsonDocument.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return new ReactivePanacheUpdateImpl(entityClass, docUpdate, collection);
    }

    private static ReactivePanacheUpdate executeUpdate(Class<?> entityClass, String update, Map<String, Object> params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        BsonDocument docUpdate = BsonDocument.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return new ReactivePanacheUpdateImpl(entityClass, docUpdate, collection);
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }

}
