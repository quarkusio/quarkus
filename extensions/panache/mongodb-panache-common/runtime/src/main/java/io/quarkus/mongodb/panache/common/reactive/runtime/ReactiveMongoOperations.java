package io.quarkus.mongodb.panache.common.reactive.runtime;

import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.beanName;
import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.clientFromArc;
import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.getDatabaseName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.jboss.logging.Logger;

import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;

import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@SuppressWarnings({ "rawtypes", "unchecked", "Convert2MethodRef" })
public abstract class ReactiveMongoOperations<QueryType, UpdateType> {
    public final String ID = "_id";
    private static final Logger LOGGER = Logger.getLogger(ReactiveMongoOperations.class);

    // update operators: https://docs.mongodb.com/manual/reference/operator/update/
    private static final List<String> UPDATE_OPERATORS = Arrays.asList(
            "$currentDate", "$inc", "$min", "$max", "$mul", "$rename", "$set", "$setOnInsert", "$unset",
            "$addToSet", "$pop", "$pull", "$push", "$pullAll",
            "$each", "$position", "$slice", "$sort",
            "$bit");

    private static final Map<String, String> defaultDatabaseName = new ConcurrentHashMap<>();

    protected abstract QueryType createQuery(ReactiveMongoCollection collection, Document query, Document sortDoc);

    protected abstract UpdateType createUpdate(ReactiveMongoCollection<?> collection, Class<?> entityClass,
            Document docUpdate);

    protected abstract Uni<?> list(QueryType query);

    protected abstract Multi<?> stream(QueryType query);

    public Uni<Void> persist(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return persist(collection, entity);
    }

    public Uni<Void> persist(Iterable<?> entities) {
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

    public Uni<Void> persist(Object firstEntity, Object... entities) {
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

    public Uni<Void> persist(Stream<?> entities) {
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

    public Uni<Void> update(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return update(collection, entity);
    }

    public Uni<Void> update(Iterable<?> entities) {
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

    public Uni<Void> update(Object firstEntity, Object... entities) {
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

    public Uni<Void> update(Stream<?> entities) {
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

    public Uni<Void> persistOrUpdate(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        return persistOrUpdate(collection, entity);
    }

    public Uni<Void> persistOrUpdate(Iterable<?> entities) {
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

    public Uni<Void> persistOrUpdate(Object firstEntity, Object... entities) {
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

    public Uni<Void> persistOrUpdate(Stream<?> entities) {
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

    public Uni<Void> delete(Object entity) {
        ReactiveMongoCollection collection = mongoCollection(entity);
        BsonDocument document = getBsonDocument(collection, entity);
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        return collection.deleteOne(query).onItem().ignore().andContinueWithNull();
    }

    public ReactiveMongoCollection mongoCollection(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        ReactiveMongoDatabase database = mongoDatabase(mongoEntity);
        if (mongoEntity != null && !mongoEntity.collection().isEmpty()) {
            return database.getCollection(mongoEntity.collection(), entityClass);
        }
        return database.getCollection(entityClass.getSimpleName(), entityClass);
    }

    public ReactiveMongoDatabase mongoDatabase(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        return mongoDatabase(mongoEntity);
    }

    //
    // Private stuff

    public Uni<Void> nullUni() {
        return Uni.createFrom().item((Void) null);
    }

    private Uni<Void> persist(ReactiveMongoCollection collection, Object entity) {
        return collection.insertOne(entity).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> persist(ReactiveMongoCollection collection, List<Object> entities) {
        return collection.insertMany(entities).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> update(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        return collection.replaceOne(query, entity).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> update(ReactiveMongoCollection collection, List<Object> entities) {
        List<Uni<Void>> unis = entities.stream().map(entity -> update(collection, entity)).collect(Collectors.toList());
        return Uni.combine().all().unis(unis).combinedWith(u -> null);
    }

    private Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            return collection.insertOne(entity).onItem().ignore().andContinueWithNull();
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            return collection.replaceOne(query, entity, new ReplaceOptions().upsert(true))
                    .onItem().ignore().andContinueWithNull();
        }
    }

    private Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, List<Object> entities) {
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

        return collection.bulkWrite(bulk).onItem().ignore().andContinueWithNull();
    }

    private BsonDocument getBsonDocument(ReactiveMongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }

    private ReactiveMongoCollection mongoCollection(Object entity) {
        Class<?> entityClass = entity.getClass();
        return mongoCollection(entityClass);
    }

    private ReactiveMongoDatabase mongoDatabase(MongoEntity mongoEntity) {
        ReactiveMongoClient mongoClient = clientFromArc(mongoEntity, ReactiveMongoClient.class, true);
        String databaseName = getDefaultDatabaseName(mongoEntity);
        return mongoClient.getDatabase(databaseName);
    }

    private String getDefaultDatabaseName(MongoEntity mongoEntity) {
        return defaultDatabaseName.computeIfAbsent(beanName(mongoEntity), new Function<String, String>() {
            @Override
            public String apply(String beanName) {
                return getDatabaseName(mongoEntity, beanName);
            }
        });
    }

    //
    // Queries

    public Uni<Object> findById(Class<?> entityClass, Object id) {
        Uni<Optional> optionalEntity = findByIdOptional(entityClass, id);
        return optionalEntity.onItem().transform(optional -> optional.orElse(null));
    }

    public Uni<Optional> findByIdOptional(Class<?> entityClass, Object id) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.find(new Document(ID, id)).collect().first()
                .onItem().transform(Optional::ofNullable);
    }

    public QueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public QueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, docQuery, docSort);
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1</code> for PanacheQL one.
     */
    public String bindFilter(Class<?> clazz, String query, Object[] params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     */
    public String bindFilter(Class<?> clazz, String query, Map<String, Object> params) {
        String bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1 and lastname = ?2</code> for PanacheQL one.
     * As update document needs an update operator, we add <code>$set</code> if none is provided.
     */
    String bindUpdate(Class<?> clazz, String query, Object[] params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!containsUpdateOperator(query)) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     * As update document needs an update operator, we add <code>$set</code> if none is provided.
     */
    String bindUpdate(Class<?> clazz, String query, Map<String, Object> params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!containsUpdateOperator(query)) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    private boolean containsUpdateOperator(String update) {
        for (String operator : UPDATE_OPERATORS) {
            if (update.contains(operator)) {
                return true;
            }
        }
        return false;
    }

    String bindQuery(Class<?> clazz, String query, Object[] params) {
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

    String bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
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

    public QueryType find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public QueryType find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, docQuery, docSort);
    }

    public QueryType find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params.map());
    }

    public QueryType find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    @SuppressWarnings("rawtypes")
    public QueryType find(Class<?> entityClass, Document query, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return createQuery(collection, query, sortDoc);
    }

    public QueryType find(Class<?> entityClass, Document query, Document sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, query, sort);
    }

    public QueryType find(Class<?> entityClass, Document query) {
        return find(entityClass, query, (Document) null);
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Object... params) {
        return (Uni) list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return (Uni) list(find(entityClass, query, sort, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return (Uni) list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return (Uni) list(find(entityClass, query, sort, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Parameters params) {
        return (Uni) list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return (Uni) list(find(entityClass, query, sort, params));
    }

    //specific Mongo query
    public Uni<List<?>> list(Class<?> entityClass, Document query) {
        return (Uni) list(find(entityClass, query));
    }

    //specific Mongo query
    public Uni<List<?>> list(Class<?> entityClass, Document query, Document sort) {
        return (Uni) list(find(entityClass, query, sort));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Object... params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Parameters params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return stream(find(entityClass, query, sort, params));
    }

    //specific Mongo query
    public Multi<?> stream(Class<?> entityClass, Document query) {
        return stream(find(entityClass, query));
    }

    //specific Mongo query
    public Multi<?> stream(Class<?> entityClass, Document query, Document sort) {
        return stream(find(entityClass, query, sort));
    }

    @SuppressWarnings("rawtypes")
    public QueryType findAll(Class<?> entityClass) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, null, null);
    }

    @SuppressWarnings("rawtypes")
    public QueryType findAll(Class<?> entityClass, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return createQuery(collection, null, sortDoc);
    }

    private Document sortToDocument(Sort sort) {
        if (sort == null) {
            return null;
        }

        Document sortDoc = new Document();
        for (Sort.Column col : sort.getColumns()) {
            sortDoc.append(col.getName(), col.getDirection() == Sort.Direction.Ascending ? 1 : -1);
        }
        return sortDoc;
    }

    public Uni<List<?>> listAll(Class<?> entityClass) {
        return (Uni) list(findAll(entityClass));
    }

    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return (Uni) list(findAll(entityClass, sort));
    }

    public Multi<?> streamAll(Class<?> entityClass) {
        return stream(findAll(entityClass));
    }

    public Multi<?> streamAll(Class<?> entityClass, Sort sort) {
        return stream(findAll(entityClass, sort));
    }

    public Uni<Long> count(Class<?> entityClass) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments();
    }

    public Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public Uni<Long> count(Class<?> entityClass, Document query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.countDocuments(query);
    }

    public Uni<Long> deleteAll(Class<?> entityClass) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(new Document()).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        return collection.deleteOne(query).map(results -> results.getDeletedCount() == 1);
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public Uni<Long> delete(Class<?> entityClass, Document query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return collection.deleteMany(query).map(deleteResult -> deleteResult.getDeletedCount());
    }

    public UpdateType update(Class<?> entityClass, String update, Map<String, Object> params) {
        return executeUpdate(entityClass, update, params);
    }

    public UpdateType update(Class<?> entityClass, String update, Parameters params) {
        return update(entityClass, update, params.map());
    }

    public UpdateType update(Class<?> entityClass, String update, Object... params) {
        return executeUpdate(entityClass, update, params);
    }

    private UpdateType executeUpdate(Class<?> entityClass, String update, Object... params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        Document docUpdate = Document.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return createUpdate(collection, entityClass, docUpdate);
    }

    private UpdateType executeUpdate(Class<?> entityClass, String update, Map<String, Object> params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        Document docUpdate = Document.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return createUpdate(collection, entityClass, docUpdate);
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }
}
