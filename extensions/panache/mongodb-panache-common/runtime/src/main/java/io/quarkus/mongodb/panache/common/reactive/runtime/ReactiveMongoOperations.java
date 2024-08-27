package io.quarkus.mongodb.panache.common.reactive.runtime;

import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.beanName;
import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.clientFromArc;
import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.getDatabaseName;
import static io.quarkus.mongodb.panache.common.runtime.BeanUtils.getDatabaseNameFromResolver;

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
import org.bson.conversions.Bson;
import org.jboss.logging.Logger;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.common.binder.PanacheQlQueryBinder;
import io.quarkus.mongodb.panache.common.reactive.Panache;
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

    protected abstract QueryType createQuery(ReactiveMongoCollection collection, Bson query, Bson sortDoc);

    protected abstract UpdateType createUpdate(ReactiveMongoCollection<?> collection, Class<?> entityClass,
            Bson docUpdate);

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

            if (!objects.isEmpty()) {
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
            if (!objects.isEmpty()) {
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

            if (!objects.isEmpty()) {
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
            if (!objects.isEmpty()) {
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

            if (!objects.isEmpty()) {
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
            if (!objects.isEmpty()) {
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

        if (Panache.getCurrentSession() != null) {
            return collection.deleteOne(Panache.getCurrentSession(), query).onItem().ignore().andContinueWithNull();
        }
        return collection.deleteOne(query).onItem().ignore().andContinueWithNull();
    }

    public ReactiveMongoCollection mongoCollection(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        ReactiveMongoDatabase database = mongoDatabase(mongoEntity);
        if (mongoEntity != null) {
            ReactiveMongoCollection collection = mongoEntity.collection().isEmpty()
                    ? database.getCollection(entityClass.getSimpleName(), entityClass)
                    : database.getCollection(mongoEntity.collection(), entityClass);
            if (!mongoEntity.readPreference().isEmpty()) {
                try {
                    collection = collection.withReadPreference(ReadPreference.valueOf(mongoEntity.readPreference()));
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException("Illegal read preference " + mongoEntity.readPreference()
                            + " configured in the @MongoEntity annotation of " + entityClass.getName() + "."
                            + " Supported values are primary|primaryPreferred|secondary|secondaryPreferred|nearest");
                }
            }
            return collection;
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
        if (Panache.getCurrentSession() != null) {
            return collection.insertOne(Panache.getCurrentSession(), entity).onItem().ignore().andContinueWithNull();
        }
        return collection.insertOne(entity).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> persist(ReactiveMongoCollection collection, List<Object> entities) {
        if (Panache.getCurrentSession() != null) {
            return collection.insertMany(Panache.getCurrentSession(), entities).onItem().ignore().andContinueWithNull();
        }
        return collection.insertMany(entities).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> update(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);

        if (Panache.getCurrentSession() != null) {
            return collection.replaceOne(Panache.getCurrentSession(), query, entity).onItem().ignore().andContinueWithNull();
        }
        return collection.replaceOne(query, entity).onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> update(ReactiveMongoCollection collection, List<Object> entities) {
        List<Uni<Void>> unis = entities.stream().map(entity -> update(collection, entity)).collect(Collectors.toList());
        return Uni.combine().all().unis(unis).with(u -> null);
    }

    private Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            if (Panache.getCurrentSession() != null) {
                return collection.insertOne(Panache.getCurrentSession(), entity).onItem().ignore().andContinueWithNull();
            }
            return collection.insertOne(entity).onItem().ignore().andContinueWithNull();
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            if (Panache.getCurrentSession() != null) {
                return collection.replaceOne(Panache.getCurrentSession(), query, entity, new ReplaceOptions().upsert(true))
                        .onItem().ignore().andContinueWithNull();
            }
            return collection.replaceOne(query, entity, new ReplaceOptions().upsert(true))
                    .onItem().ignore().andContinueWithNull();
        }
    }

    private Uni<Void> persistOrUpdate(ReactiveMongoCollection collection, List<Object> entities) {
        //this will be an ordered bulk: it's less performant than an unordered one but will fail at the first failed write
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

        if (Panache.getCurrentSession() != null) {
            return collection.bulkWrite(Panache.getCurrentSession(), bulk).onItem().ignore().andContinueWithNull();
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
        if (mongoEntity != null && !mongoEntity.database().isEmpty()) {
            return mongoClient.getDatabase(mongoEntity.database());
        }
        String databaseName = getDatabaseNameFromResolver().orElseGet(() -> getDefaultDatabaseName(mongoEntity));
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
        if (Panache.getCurrentSession() != null) {
            return collection.find(Panache.getCurrentSession(), new Document(ID, id)).collect().first()
                    .onItem().transform(Optional::ofNullable);
        }
        return collection.find(new Document(ID, id)).collect().first()
                .onItem().transform(Optional::ofNullable);
    }

    public QueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    public QueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        Bson docQuery = Document.parse(bindQuery);
        Bson docSort = sortToDocument(sort);
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

    public QueryType find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        Bson docQuery = Document.parse(bindQuery);
        Bson docSort = sortToDocument(sort);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, docQuery, docSort);
    }

    public QueryType find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params.map());
    }

    public QueryType find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    public QueryType find(Class<?> entityClass, Bson query, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Bson sortDoc = sortToDocument(sort);
        return createQuery(collection, query, sortDoc);
    }

    public QueryType find(Class<?> entityClass, Bson query, Bson sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, query, sort);
    }

    public QueryType find(Class<?> entityClass, Bson query) {
        return find(entityClass, query, (Bson) null);
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
    public Uni<List<?>> list(Class<?> entityClass, Bson query) {
        return (Uni) list(find(entityClass, query));
    }

    //specific Mongo query
    public Uni<List<?>> list(Class<?> entityClass, Bson query, Bson sort) {
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
    public Multi<?> stream(Class<?> entityClass, Bson query) {
        return stream(find(entityClass, query));
    }

    //specific Mongo query
    public Multi<?> stream(Class<?> entityClass, Bson query, Bson sort) {
        return stream(find(entityClass, query, sort));
    }

    public QueryType findAll(Class<?> entityClass) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, null, null);
    }

    public QueryType findAll(Class<?> entityClass, Sort sort) {
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        Bson sortDoc = sortToDocument(sort);
        return createQuery(collection, null, sortDoc);
    }

    private Bson sortToDocument(Sort sort) {
        if (sort == null) {
            return null;
        }

        Document sortDoc = new Document();
        for (Sort.Column col : sort.getColumns()) {
            sortDoc.append(col.getName(), col.getDirection() == Sort.Direction.Ascending ? 1 : -1);
            if (col.getNullPrecedence() != null) {
                throw new UnsupportedOperationException("Cannot sort by nulls first or nulls last");
            }
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
        if (Panache.getCurrentSession() != null) {
            return collection.countDocuments(Panache.getCurrentSession());
        }
        return collection.countDocuments();
    }

    public Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.countDocuments(Panache.getCurrentSession(), docQuery);
        }
        return collection.countDocuments(docQuery);
    }

    public Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.countDocuments(Panache.getCurrentSession(), docQuery);
        }
        return collection.countDocuments(docQuery);
    }

    public Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public Uni<Long> count(Class<?> entityClass, Bson query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.countDocuments(Panache.getCurrentSession(), query);
        }
        return collection.countDocuments(query);
    }

    public Uni<Long> deleteAll(Class<?> entityClass) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.deleteMany(Panache.getCurrentSession(), new Document()).map(DeleteResult::getDeletedCount);
        }
        return collection.deleteMany(new Document()).map(DeleteResult::getDeletedCount);
    }

    public Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        Bson query = new Document().append(ID, id);
        if (Panache.getCurrentSession() != null) {
            return collection.deleteOne(Panache.getCurrentSession(), query).map(results -> results.getDeletedCount() == 1);
        }
        return collection.deleteOne(query).map(results -> results.getDeletedCount() == 1);
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.deleteMany(Panache.getCurrentSession(), docQuery).map(DeleteResult::getDeletedCount);
        }
        return collection.deleteMany(docQuery).map(DeleteResult::getDeletedCount);
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.deleteMany(Panache.getCurrentSession(), docQuery).map(DeleteResult::getDeletedCount);
        }
        return collection.deleteMany(docQuery).map(DeleteResult::getDeletedCount);
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public Uni<Long> delete(Class<?> entityClass, Bson query) {
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        if (Panache.getCurrentSession() != null) {
            return collection.deleteMany(Panache.getCurrentSession(), query).map(DeleteResult::getDeletedCount);
        }
        return collection.deleteMany(query).map(DeleteResult::getDeletedCount);
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

    public UpdateType update(Class<?> entityClass, Bson update) {
        return createUpdate(mongoCollection(entityClass), entityClass, update);
    }

    private UpdateType executeUpdate(Class<?> entityClass, String update, Object... params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        Bson docUpdate = Document.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return createUpdate(collection, entityClass, docUpdate);
    }

    private UpdateType executeUpdate(Class<?> entityClass, String update, Map<String, Object> params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        Bson docUpdate = Document.parse(bindUpdate);
        ReactiveMongoCollection<?> collection = mongoCollection(entityClass);
        return createUpdate(collection, entityClass, docUpdate);
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }
}
