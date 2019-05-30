/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class TransactionScopedEntityManager implements EntityManager {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";
    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry tsr;
    private final EntityManagerFactory emf;
    private final String unitName;
    private static final Object transactionKey = new Object();
    private final Instance<RequestScopedEntityManagerHolder> requestScopedEms;

    public TransactionScopedEntityManager(TransactionManager transactionManager,
            TransactionSynchronizationRegistry tsr,
            EntityManagerFactory emf,
            String unitName, Instance<RequestScopedEntityManagerHolder> requestScopedEms) {
        this.transactionManager = transactionManager;
        this.tsr = tsr;
        this.emf = emf;
        this.unitName = unitName;
        this.requestScopedEms = requestScopedEms;
    }

    EntityManagerResult getEntityManager() {
        if (isInTransaction()) {
            EntityManager em = (EntityManager) tsr.getResource(transactionKey);
            if (em != null) {
                return new EntityManagerResult(em, false, true);
            }
            EntityManager newEm = emf.createEntityManager();
            newEm.joinTransaction();
            tsr.putResource(transactionKey, newEm);
            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    newEm.flush();
                    newEm.close();
                }

                @Override
                public void afterCompletion(int i) {
                    newEm.close();
                }
            });
            return new EntityManagerResult(newEm, false, true);
        } else {
            //this will throw an exception if the request scope is not active
            //this is expected as either the request scope or an active transaction
            //is required to properly managed the EM lifecycle
            RequestScopedEntityManagerHolder requestScopedEms = this.requestScopedEms.get();
            return new EntityManagerResult(requestScopedEms.getOrCreateEntityManager(unitName, emf), false, false);
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
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.persist(entity);
        }
    }

    @Override
    public <T> T merge(T entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            return emr.em.merge(entity);
        }
    }

    @Override
    public void remove(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.remove(entity);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.find(entityClass, primaryKey);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.find(entityClass, primaryKey, properties);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.find(entityClass, primaryKey, lockMode);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.find(entityClass, primaryKey, lockMode, properties);
        }
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getReference(entityClass, primaryKey);
        }
    }

    @Override
    public void flush() {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.flush();
        }
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.em.setFlushMode(flushMode);
        }
    }

    @Override
    public FlushModeType getFlushMode() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getFlushMode();
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.lock(entity, lockMode);
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.lock(entity, lockMode, properties);
        }
    }

    @Override
    public void refresh(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.refresh(entity);
        }
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.refresh(entity, properties);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.refresh(entity, lockMode);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        try (EntityManagerResult emr = getEntityManager()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.em.refresh(entity, lockMode, properties);
        }
    }

    @Override
    public void clear() {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.em.clear();
        }
    }

    @Override
    public void detach(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.em.detach(entity);
        }
    }

    @Override
    public boolean contains(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.contains(entity);
        }
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getLockMode(entity);
        }
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.em.setProperty(propertyName, value);
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getProperties();
        }
    }

    @Override
    public Query createQuery(String qlString) {
        //TODO: this needs some thought for how it works outside a tx
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createQuery(qlString);
        }
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createQuery(criteriaQuery);
        }
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createQuery(updateQuery);
        }
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createQuery(deleteQuery);
        }
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createQuery(qlString, resultClass);
        }
    }

    @Override
    public Query createNamedQuery(String name) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNamedQuery(name);
        }
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNamedQuery(name, resultClass);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNativeQuery(sqlString);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNativeQuery(sqlString, resultClass);
        }
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNativeQuery(sqlString, resultSetMapping);
        }
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createNamedStoredProcedureQuery(name);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createStoredProcedureQuery(procedureName);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createStoredProcedureQuery(procedureName, resultSetMappings);
        }
    }

    @Override
    public void joinTransaction() {
        try (EntityManagerResult emr = getEntityManager()) {
            emr.em.joinTransaction();
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.isJoinedToTransaction();
        }
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.unwrap(cls);
        }
    }

    @Override
    public Object getDelegate() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getDelegate();
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
            return emr.em.getEntityManagerFactory();
        }
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getCriteriaBuilder();
        }
    }

    @Override
    public Metamodel getMetamodel() {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getMetamodel();
        }
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createEntityGraph(rootType);
        }
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.createEntityGraph(graphName);
        }
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getEntityGraph(graphName);
        }
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        try (EntityManagerResult emr = getEntityManager()) {
            return emr.em.getEntityGraphs(entityClass);
        }
    }

    static class EntityManagerResult implements AutoCloseable {

        final EntityManager em;
        final boolean closeOnEnd;
        final boolean allowModification;

        EntityManagerResult(EntityManager em, boolean closeOnEnd, boolean allowModification) {
            this.em = em;
            this.closeOnEnd = closeOnEnd;
            this.allowModification = allowModification;
        }

        @Override
        public void close() {
            if (closeOnEnd) {
                em.close();
            }
        }
    }
}
