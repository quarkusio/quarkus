package io.quarkus.hibernate.orm.runtime.entitymanager;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.quarkus.hibernate.orm.runtime.RequestScopedEntityManagerHolder;
import io.quarkus.runtime.BlockingOperationControl;

public class TransactionScopedEntityManager implements EntityManager {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final EntityManagerFactory entityManagerFactory;
    private final String unitName;
    private final String entityManagerKey;
    private final Instance<RequestScopedEntityManagerHolder> requestScopedEntityManagers;

    public TransactionScopedEntityManager(TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            EntityManagerFactory entityManagerFactory,
            String unitName,
            Instance<RequestScopedEntityManagerHolder> requestScopedEntityManagers) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.entityManagerFactory = entityManagerFactory;
        this.unitName = unitName;
        this.entityManagerKey = this.getClass().getSimpleName() + "-" + unitName;
        this.requestScopedEntityManagers = requestScopedEntityManagers;
    }

    EntityManagerResult getEntityManager() {
        if (isInTransaction()) {
            EntityManager entityManager = (EntityManager) transactionSynchronizationRegistry.getResource(entityManagerKey);
            if (entityManager != null) {
                return new EntityManagerResult(entityManager, false, true);
            }
            EntityManager newEntityManager = entityManagerFactory.createEntityManager();
            newEntityManager.joinTransaction();
            transactionSynchronizationRegistry.putResource(entityManagerKey, newEntityManager);
            transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    newEntityManager.flush();
                }

                @Override
                public void afterCompletion(int i) {
                    newEntityManager.close();
                }
            });
            return new EntityManagerResult(newEntityManager, false, true);
        } else {
            //this will throw an exception if the request scope is not active
            //this is expected as either the request scope or an active transaction
            //is required to properly managed the EM lifecycle
            RequestScopedEntityManagerHolder requestScopedEntityManagers = this.requestScopedEntityManagers.get();
            return new EntityManagerResult(requestScopedEntityManagers.getOrCreateEntityManager(unitName, entityManagerFactory),
                    false, false);
        }
    }

    private void checkBlocking() {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new IllegalStateException(
                    "You have attempted to perform a blocking operation on a IO thread. This is not allowed, as blocking the IO thread will cause major performance issues with your application. If you want to perform blocking EntityManager operations make sure you are doing it from a worker thread.");
        }
    }

    private boolean isInTransaction() {
        try {
            switch (transactionManager.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(Object entity) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.persist(entity);
        }
    }

    @Override
    public <T> T merge(T entity) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            return emr.entityManager.merge(entity);
        }
    }

    @Override
    public void remove(Object entity) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.remove(entity);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.find(entityClass, primaryKey);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.find(entityClass, primaryKey, properties);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.find(entityClass, primaryKey, lockMode);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.find(entityClass, primaryKey, lockMode, properties);
        }
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getReference(entityClass, primaryKey);
        }
    }

    @Override
    public void flush() {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.flush();
        }
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.entityManager.setFlushMode(flushMode);
        }
    }

    @Override
    public FlushModeType getFlushMode() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getFlushMode();
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.lock(entity, lockMode);
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.lock(entity, lockMode, properties);
        }
    }

    @Override
    public void refresh(Object entity) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.refresh(entity);
        }
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.refresh(entity, properties);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.refresh(entity, lockMode);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.entityManager.refresh(entity, lockMode, properties);
        }
    }

    @Override
    public void clear() {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.entityManager.clear();
        }
    }

    @Override
    public void detach(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.entityManager.detach(entity);
        }
    }

    @Override
    public boolean contains(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.contains(entity);
        }
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getLockMode(entity);
        }
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.entityManager.setProperty(propertyName, value);
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getProperties();
        }
    }

    @Override
    public Query createQuery(String qlString) {
        checkBlocking();
        //TODO: this needs some thought for how it works outside a tx
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createQuery(qlString);
        }
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createQuery(criteriaQuery);
        }
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createQuery(updateQuery);
        }
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createQuery(deleteQuery);
        }
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createQuery(qlString, resultClass);
        }
    }

    @Override
    public Query createNamedQuery(String name) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNamedQuery(name);
        }
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNamedQuery(name, resultClass);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNativeQuery(sqlString);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNativeQuery(sqlString, resultClass);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNativeQuery(sqlString, resultSetMapping);
        }
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createNamedStoredProcedureQuery(name);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createStoredProcedureQuery(procedureName);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
        }
    }

    @Override
    public void joinTransaction() {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.entityManager.joinTransaction();
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.isJoinedToTransaction();
        }
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.unwrap(cls);
        }
    }

    @Override
    public Object getDelegate() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getDelegate();
        }
    }

    @Override
    public void close() {
        throw new IllegalStateException("Not supported for transaction scoped entity managers");
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public EntityTransaction getTransaction() {
        throw new IllegalStateException("Not supported for JTA entity managers");
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getEntityManagerFactory();
        }
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        checkBlocking();
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getCriteriaBuilder();
        }
    }

    @Override
    public Metamodel getMetamodel() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getMetamodel();
        }
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createEntityGraph(rootType);
        }
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.createEntityGraph(graphName);
        }
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getEntityGraph(graphName);
        }
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.entityManager.getEntityGraphs(entityClass);
        }
    }

    static class EntityManagerResult implements AutoCloseable {

        final EntityManager entityManager;
        final boolean closeOnEnd;
        final boolean allowModification;

        EntityManagerResult(EntityManager entityManager, boolean closeOnEnd, boolean allowModification) {
            this.entityManager = entityManager;
            this.closeOnEnd = closeOnEnd;
            this.allowModification = allowModification;
        }

        @Override
        public void close() {
            if (closeOnEnd) {
                entityManager.close();
            }
        }
    }
}
