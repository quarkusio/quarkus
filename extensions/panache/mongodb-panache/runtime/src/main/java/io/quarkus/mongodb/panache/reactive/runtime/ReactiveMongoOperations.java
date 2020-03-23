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

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
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
        return collection.deleteOne(query).onItem().ignore().andContinueWithNull();
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
        return collection.insertOne(entity);
    }

    private static Uni<Void> persist(ReactiveMongoCollection collection, List<Object> entities) {
        return collection.insertMany(entities);
    }

    private static Uni<Void> update(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        return collection.replaceOne(query, entity).onItem().ignore().andContinueWithNull();
    }

    private static Uni<Void> update(ReactiveMongoCollection collection, List<Object> entities) {
        Uni<Void> ret = nullUni();
        for (Object entity : entities) {
            ret.and(update(collection, entity));
        }
        return ret.onItem().ignore().andContinueWithNull();
    }

    private static Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            return collection.insertOne(entity);
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            return collection.replaceOne(query, entity, ReplaceOptions.createReplaceOptions(new UpdateOptions().upsert(true)))
                    .onItem().ignore().andContinueWithNull();
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
                        ReplaceOptions.createReplaceOptions(new UpdateOptions().upsert(true))));
            }
        }

        return collection.bulkWrite(bulk).onItem().ignore().andContinueWithNull();
    }

    private static BsonDocument getBsonDocument(ReactiveMongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
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
        String databaseName = ConfigProvider.getConfig()
                .getValue(MONGODB_DATABASE, String.class);
        return mongoClient.getDatabase(databaseName);
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
        return collection.find(new Document(ID, id)).collectItems().first()
                .onItem().apply(Optional::ofNullable);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, entityClass, docQuery, docSort);
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1</code> for PanacheQL one.
     */
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

        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     */
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

        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, entityClass, docQuery, docSort);
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
        return new ReactivePanacheQueryImpl(collection, entityClass, query, sortDoc);
    }

    public static ReactivePanacheQuery<?> find(Class<?> entityClass, Document query, Document sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return new ReactivePanacheQueryImpl(collection, entityClass, query, sort);
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
        return new ReactivePanacheQueryImpl(collection, entityClass, null, null);
    }

    @SuppressWarnings("rawtypes")
    public static ReactivePanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return new ReactivePanacheQueryImpl(collection, entityClass, null, sortDoc);
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
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments();
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public static Uni<Long> count(Class<?> entityClass, Document query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.countDocuments(query);
    }

    public static Uni<Long> deleteAll(Class<?> entityClass) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(new Document()).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public static Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        return collection.deleteOne(query).map(results -> results.getDeletedCount() == 1);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindQuery(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public static Uni<Long> delete(Class<?> entityClass, Document query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(query).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }

}
