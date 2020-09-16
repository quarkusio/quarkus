package io.quarkus.mongodb.panache.runtime;

import static io.quarkus.mongodb.panache.runtime.BeanUtils.beanName;
import static io.quarkus.mongodb.panache.runtime.BeanUtils.clientFromArc;
import static io.quarkus.mongodb.panache.runtime.BeanUtils.getDatabaseName;

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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class MongoOperations<QueryType, UpdateType> {
    public final String ID = "_id";
    private static final Logger LOGGER = Logger.getLogger(MongoOperations.class);

    private final Map<String, String> defaultDatabaseName = new ConcurrentHashMap<>();

    protected abstract QueryType createQuery(MongoCollection<?> collection, Document query, Document sortDoc);

    protected abstract UpdateType createUpdate(MongoCollection collection, Class<?> entityClass, Document docUpdate);

    protected abstract List<?> list(QueryType queryType);

    protected abstract Stream<?> stream(QueryType queryType);

    public void persist(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        persist(collection, entity);
    }

    public void persist(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persist(collection, objects);
        }
    }

    public void persist(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            persist(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persist(collection, entityList);
        }
    }

    public void persist(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            update(collection, objects);
        }
    }

    public void update(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        update(collection, entity);
    }

    public void update(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            update(collection, objects);
        }
    }

    public void update(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            update(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            update(collection, entityList);
        }
    }

    public void update(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            update(collection, objects);
        }
    }

    public void persistOrUpdate(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        persistOrUpdate(collection, entity);
    }

    public void persistOrUpdate(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persistOrUpdate(collection, objects);
        }
    }

    public void persistOrUpdate(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            persistOrUpdate(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persistOrUpdate(collection, entityList);
        }
    }

    public void persistOrUpdate(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (objects.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = objects.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            persistOrUpdate(collection, objects);
        }
    }

    public void delete(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        BsonDocument document = getBsonDocument(collection, entity);
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        collection.deleteOne(query);
    }

    public MongoCollection mongoCollection(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        MongoDatabase database = mongoDatabase(mongoEntity);
        if (mongoEntity != null && !mongoEntity.collection().isEmpty()) {
            return database.getCollection(mongoEntity.collection(), entityClass);
        }
        return database.getCollection(entityClass.getSimpleName(), entityClass);
    }

    public MongoDatabase mongoDatabase(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        return mongoDatabase(mongoEntity);
    }

    //
    // Private stuff

    private void persist(MongoCollection collection, Object entity) {
        collection.insertOne(entity);
    }

    private void persist(MongoCollection collection, List<Object> entities) {
        collection.insertMany(entities);
    }

    private void update(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        collection.replaceOne(query, entity);
    }

    private void update(MongoCollection collection, List<Object> entities) {
        for (Object entity : entities) {
            update(collection, entity);
        }
    }

    private void persistOrUpdate(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            collection.insertOne(entity);
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            collection.replaceOne(query, entity, new ReplaceOptions().upsert(true));
        }
    }

    private void persistOrUpdate(MongoCollection collection, List<Object> entities) {
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

        collection.bulkWrite(bulk);
    }

    private BsonDocument getBsonDocument(MongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }

    private MongoCollection mongoCollection(Object entity) {
        Class<?> entityClass = entity.getClass();
        return mongoCollection(entityClass);
    }

    private MongoDatabase mongoDatabase(MongoEntity entity) {
        MongoClient mongoClient = clientFromArc(entity, MongoClient.class, false);
        if (entity != null && !entity.database().isEmpty()) {
            return mongoClient.getDatabase(entity.database());
        }
        String databaseName = getDefaultDatabaseName(entity);
        return mongoClient.getDatabase(databaseName);
    }

    private String getDefaultDatabaseName(MongoEntity entity) {
        return defaultDatabaseName.computeIfAbsent(beanName(entity), new Function<String, String>() {
            @Override
            public String apply(String beanName) {
                return getDatabaseName(entity, beanName);
            }
        });
    }
    //
    // Queries

    public Object findById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.find(new Document(ID, id)).first();
    }

    public Optional findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public QueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public QueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        Document docQuery = Document.parse(bindQuery);
        Document docSort = sortToDocument(sort);
        MongoCollection collection = mongoCollection(entityClass);
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
     * As update document needs a <code>$set</code> operator we add it if needed.
     */
    String bindUpdate(Class<?> clazz, String query, Object[] params) {
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
    String bindUpdate(Class<?> clazz, String query, Map<String, Object> params) {
        String bindUpdate = bindQuery(clazz, query, params);
        if (!bindUpdate.contains("$set")) {
            bindUpdate = "{'$set':" + bindUpdate + "}";
        }
        LOGGER.debug(bindUpdate);
        return bindUpdate;
    }

    private String bindQuery(Class<?> clazz, String query, Object[] params) {
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

    private String bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
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
        MongoCollection collection = mongoCollection(entityClass);
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
        MongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        return createQuery(collection, query, sortDoc);
    }

    public QueryType find(Class<?> entityClass, Document query, Document sort) {
        MongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, query, sort);
    }

    public QueryType find(Class<?> entityClass, Document query) {
        return find(entityClass, query, (Document) null);
    }

    public List<?> list(Class<?> entityClass, String query, Object... params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return list(find(entityClass, query, sort, params));
    }

    public List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return list(find(entityClass, query, sort, params));
    }

    public List<?> list(Class<?> entityClass, String query, Parameters params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return list(find(entityClass, query, sort, params));
    }

    //specific Mongo query
    public List<?> list(Class<?> entityClass, Document query) {
        return list(find(entityClass, query));
    }

    //specific Mongo query
    public List<?> list(Class<?> entityClass, Document query, Document sort) {
        return list(find(entityClass, query, sort));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return stream(find(entityClass, query, sort, params));
    }

    //specific Mongo query
    public Stream<?> stream(Class<?> entityClass, Document query) {
        return stream(find(entityClass, query));
    }

    //specific Mongo query
    public Stream<?> stream(Class<?> entityClass, Document query, Document sort) {
        return stream(find(entityClass, query, sort));
    }

    @SuppressWarnings("rawtypes")
    public QueryType findAll(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return createQuery(collection, null, null);
    }

    @SuppressWarnings("rawtypes")
    public QueryType findAll(Class<?> entityClass, Sort sort) {
        MongoCollection collection = mongoCollection(entityClass);
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

    public List<?> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public List<?> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    public Stream<?> streamAll(Class<?> entityClass) {
        return stream(findAll(entityClass));
    }

    public Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return stream(findAll(entityClass, sort));
    }

    public long count(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments();
    }

    public long count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public long count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(docQuery);
    }

    public long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public long count(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.countDocuments(query);
    }

    public long deleteAll(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(new Document()).getDeletedCount();
    }

    public boolean deleteById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        DeleteResult results = collection.deleteOne(query);
        return results.getDeletedCount() == 1;
    }

    public long delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).getDeletedCount();
    }

    public long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(docQuery).getDeletedCount();
    }

    public long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public long delete(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        return collection.deleteMany(query).getDeletedCount();
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
        return createUpdate(mongoCollection(entityClass), entityClass, docUpdate);
    }

    private UpdateType executeUpdate(Class<?> entityClass, String update, Map<String, Object> params) {
        String bindUpdate = bindUpdate(entityClass, update, params);
        Document docUpdate = Document.parse(bindUpdate);
        return createUpdate(mongoCollection(entityClass), entityClass, docUpdate);
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }

}
