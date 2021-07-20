package io.quarkus.mongodb.panache.common.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.jboss.logging.Logger;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.binder.NativeQueryBinder;
import io.quarkus.mongodb.panache.binder.PanacheQlQueryBinder;
import io.quarkus.mongodb.panache.transaction.MongoTransactionException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class MongoOperations<QueryType, UpdateType> {
    public static final String ID = "_id";
    public static final Object SESSION_KEY = new Object();

    private static final Logger LOGGER = Logger.getLogger(MongoOperations.class);

    // update operators: https://docs.mongodb.com/manual/reference/operator/update/
    private static final List<String> UPDATE_OPERATORS = Arrays.asList(
            "$currentDate", "$inc", "$min", "$max", "$mul", "$rename", "$set", "$setOnInsert", "$unset",
            "$addToSet", "$pop", "$pull", "$push", "$pullAll",
            "$each", "$position", "$slice", "$sort",
            "$bit");

    private final Map<String, String> defaultDatabaseName = new ConcurrentHashMap<>();

    protected abstract QueryType createQuery(MongoCollection<?> collection, ClientSession session, Document query,
            Document sortDoc);

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

        persist(objects);
    }

    public void persist(Object firstEntity, Object... entities) {
        if (entities == null || entities.length == 0) {
            MongoCollection collection = mongoCollection(firstEntity);
            persist(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persist(entityList);
        }
    }

    public void persist(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        persist(objects);
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

        persistOrUpdate(objects);
    }

    public void persistOrUpdate(Object firstEntity, Object... entities) {
        MongoCollection collection = mongoCollection(firstEntity);
        if (entities == null || entities.length == 0) {
            persistOrUpdate(collection, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persistOrUpdate(entityList);
        }
    }

    public void persistOrUpdate(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        persistOrUpdate(objects);
    }

    public void delete(Object entity) {
        MongoCollection collection = mongoCollection(entity);
        BsonDocument document = getBsonDocument(collection, entity);
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        ClientSession session = getSession(entity);
        if (session == null) {
            collection.deleteOne(query);
        } else {
            collection.deleteOne(session, query);
        }
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
        ClientSession session = getSession(entity);
        if (session == null) {
            collection.insertOne(entity);
        } else {
            collection.insertOne(session, entity);
        }
    }

    private void persist(List<Object> entities) {
        if (entities.size() > 0) {
            // get the first entity to be able to retrieve the collection with it
            Object firstEntity = entities.get(0);
            MongoCollection collection = mongoCollection(firstEntity);
            ClientSession session = getSession(firstEntity);
            if (session == null) {
                collection.insertMany(entities);
            } else {
                collection.insertMany(session, entities);
            }
        }
    }

    private void update(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        BsonDocument query = new BsonDocument().append(ID, id);
        ClientSession session = getSession(entity);
        if (session == null) {
            collection.replaceOne(query, entity);
        } else {
            collection.replaceOne(session, query, entity);
        }
    }

    private void update(MongoCollection collection, List<Object> entities) {
        for (Object entity : entities) {
            update(collection, entity);
        }
    }

    private void persistOrUpdate(MongoCollection collection, Object entity) {
        //we transform the entity as a document first
        BsonDocument document = getBsonDocument(collection, entity);

        ClientSession session = getSession(entity);
        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(ID);
        if (id == null) {
            //insert with autogenerated ID
            if (session == null) {
                collection.insertOne(entity);
            } else {
                collection.insertOne(session, entity);
            }
        } else {
            //insert with user provided ID or update
            BsonDocument query = new BsonDocument().append(ID, id);
            if (session == null) {
                collection.replaceOne(query, entity, new ReplaceOptions().upsert(true));
            } else {
                collection.replaceOne(session, query, entity, new ReplaceOptions().upsert(true));
            }
        }
    }

    private void persistOrUpdate(List<Object> entities) {
        if (entities.isEmpty()) {
            return;
        }

        // get the first entity to be able to retrieve the collection with it
        Object firstEntity = entities.get(0);
        MongoCollection collection = mongoCollection(firstEntity);
        ClientSession session = getSession(firstEntity);

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

        if (session == null) {
            collection.bulkWrite(bulk);
        } else {
            collection.bulkWrite(session, bulk);
        }
    }

    private BsonDocument getBsonDocument(MongoCollection collection, Object entity) {
        BsonDocument document = new BsonDocument();
        Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }

    ClientSession getSession(Object entity) {
        return getSession(entity.getClass());
    }

    ClientSession getSession(Class<?> entityClass) {
        MongoEntity mongoEntity = entityClass.getAnnotation(MongoEntity.class);
        InstanceHandle<TransactionSynchronizationRegistry> instance = Arc.container()
                .instance(TransactionSynchronizationRegistry.class);
        if (instance.isAvailable()) {
            TransactionSynchronizationRegistry registry = instance.get();
            if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
                ClientSession clientSession = (ClientSession) registry.getResource(SESSION_KEY);
                if (clientSession == null) {
                    return registerClientSession(mongoEntity, registry);
                }
            }
        }
        return null;
    }

    private ClientSession registerClientSession(MongoEntity mongoEntity, TransactionSynchronizationRegistry registry) {
        TransactionManager transactionManager = Arc.container().instance(TransactionManager.class).get();
        MongoClient client = BeanUtils.clientFromArc(mongoEntity, MongoClient.class, false);
        ClientSession clientSession = client.startSession();
        clientSession.startTransaction();//TODO add txoptions from annotation
        registry.putResource(SESSION_KEY, clientSession);
        registry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int i) {
                try {
                    if (transactionManager.getStatus() == Status.STATUS_ROLLEDBACK) {
                        try {
                            clientSession.abortTransaction();
                        } finally {
                            clientSession.close();
                        }
                    } else {
                        try {
                            clientSession.commitTransaction();
                        } finally {
                            clientSession.close();
                        }
                    }
                } catch (SystemException e) {
                    throw new MongoTransactionException(e);
                }
            }
        });
        return clientSession;
    }

    private MongoCollection mongoCollection(Object entity) {
        Class<?> entityClass = entity.getClass();
        return mongoCollection(entityClass);
    }

    private MongoDatabase mongoDatabase(MongoEntity entity) {
        MongoClient mongoClient = BeanUtils.clientFromArc(entity, MongoClient.class, false);
        if (entity != null && !entity.database().isEmpty()) {
            return mongoClient.getDatabase(entity.database());
        }
        String databaseName = getDefaultDatabaseName(entity);
        return mongoClient.getDatabase(databaseName);
    }

    private String getDefaultDatabaseName(MongoEntity entity) {
        return defaultDatabaseName.computeIfAbsent(BeanUtils.beanName(entity), new Function<String, String>() {
            @Override
            public String apply(String beanName) {
                return BeanUtils.getDatabaseName(entity, beanName);
            }
        });
    }
    //
    // Queries

    public Object findById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.find(new Document(ID, id)).first()
                : collection.find(session, new Document(ID, id)).first();
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
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, docQuery, docSort);
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
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, docQuery, docSort);
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
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, query, sortDoc);
    }

    public QueryType find(Class<?> entityClass, Document query, Document sort) {
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, query, sort);
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
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, null, null);
    }

    @SuppressWarnings("rawtypes")
    public QueryType findAll(Class<?> entityClass, Sort sort) {
        MongoCollection collection = mongoCollection(entityClass);
        Document sortDoc = sortToDocument(sort);
        ClientSession session = getSession(entityClass);
        return createQuery(collection, session, null, sortDoc);
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
        ClientSession session = getSession(entityClass);
        return session == null ? collection.countDocuments() : collection.countDocuments(session);
    }

    public long count(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);

        ClientSession session = getSession(entityClass);
        return session == null ? collection.countDocuments(docQuery) : collection.countDocuments(session, docQuery);
    }

    public long count(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);

        ClientSession session = getSession(entityClass);
        return session == null ? collection.countDocuments(docQuery) : collection.countDocuments(session, docQuery);
    }

    public long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    //specific Mongo query
    public long count(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.countDocuments(query) : collection.countDocuments(session, query);
    }

    public long deleteAll(Class<?> entityClass) {
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.deleteMany(new Document()).getDeletedCount()
                : collection.deleteMany(session, new Document()).getDeletedCount();
    }

    public boolean deleteById(Class<?> entityClass, Object id) {
        MongoCollection collection = mongoCollection(entityClass);
        Document query = new Document().append(ID, id);
        ClientSession session = getSession(entityClass);
        DeleteResult results = session == null ? collection.deleteOne(query) : collection.deleteOne(session, query);
        return results.getDeletedCount() == 1;
    }

    public long delete(Class<?> entityClass, String query, Object... params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.deleteMany(docQuery).getDeletedCount()
                : collection.deleteMany(session, docQuery).getDeletedCount();
    }

    public long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        String bindQuery = bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.deleteMany(docQuery).getDeletedCount()
                : collection.deleteMany(session, docQuery).getDeletedCount();
    }

    public long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    //specific Mongo query
    public long delete(Class<?> entityClass, Document query) {
        MongoCollection collection = mongoCollection(entityClass);
        ClientSession session = getSession(entityClass);
        return session == null ? collection.deleteMany(query).getDeletedCount()
                : collection.deleteMany(session, query).getDeletedCount();
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
