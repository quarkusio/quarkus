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

package io.quarkus.arc.test.injection.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ResourceInjectionTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(EEResourceField.class, JpaClient.class)
            .resourceReferenceProviders(EntityManagerProvider.class, DummyProvider.class).resourceAnnotations(PersistenceContext.class, Dummy.class).build();

    @Test
    public void testInjection() {
        DummyProvider.DUMMY_DESTROYED.set(false);

        InstanceHandle<JpaClient> handle = Arc.container().instance(JpaClient.class);
        JpaClient client = handle.get();
        assertNotNull(client.entityManager);
        assertFalse(client.entityManager.isOpen());
        assertEquals("05", client.dummyString);

        assertFalse(DummyProvider.DUMMY_DESTROYED.get());
        handle.destroy();
        assertTrue(DummyProvider.DUMMY_DESTROYED.get());
    }

    @Dependent
    static class JpaClient {

        @Dummy
        String dummyString;

        @Inject
        EntityManager entityManager;

    }

    @Singleton
    static class EEResourceField {

        @Produces
        @PersistenceContext
        EntityManager entityManager;

    }

    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Dummy {
    }

    public static class DummyProvider implements ResourceReferenceProvider {

        static final AtomicBoolean DUMMY_DESTROYED = new AtomicBoolean();

        @Override
        public InstanceHandle<Object> get(Type type, Set<Annotation> annotations) {
            if (String.class.equals(type) && getAnnotation(annotations, Dummy.class) != null) {
                return new InstanceHandle<Object>() {
                    @Override
                    public String get() {
                        return "05";
                    }

                    @Override
                    public void destroy() {
                        DUMMY_DESTROYED.set(true);
                    }
                };
            }
            return null;
        }

    }

    @SuppressWarnings("rawtypes")
    public static class EntityManagerProvider implements ResourceReferenceProvider {

        @Override
        public InstanceHandle<Object> get(Type type, Set<Annotation> annotations) {
            if (EntityManager.class.equals(type)) {
                EntityManager entityManager = new EntityManager() {

                    @Override
                    public <T> T unwrap(Class<T> cls) {
                        return null;
                    }

                    @Override
                    public void setProperty(String propertyName, Object value) {
                    }

                    @Override
                    public void setFlushMode(FlushModeType flushMode) {
                    }

                    @Override
                    public void remove(Object entity) {
                    }

                    @Override
                    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
                    }

                    @Override
                    public void refresh(Object entity, LockModeType lockMode) {
                    }

                    @Override
                    public void refresh(Object entity, Map<String, Object> properties) {
                    }

                    @Override
                    public void refresh(Object entity) {
                    }

                    @Override
                    public void persist(Object entity) {
                    }

                    @Override
                    public <T> T merge(T entity) {
                        return null;
                    }

                    @Override
                    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
                    }

                    @Override
                    public void lock(Object entity, LockModeType lockMode) {
                    }

                    @Override
                    public void joinTransaction() {
                    }

                    @Override
                    public boolean isOpen() {
                        return false;
                    }

                    @Override
                    public boolean isJoinedToTransaction() {
                        return false;
                    }

                    @Override
                    public EntityTransaction getTransaction() {
                        return null;
                    }

                    @Override
                    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
                        return null;
                    }

                    @Override
                    public Map<String, Object> getProperties() {
                        return null;
                    }

                    @Override
                    public Metamodel getMetamodel() {
                        return null;
                    }

                    @Override
                    public LockModeType getLockMode(Object entity) {
                        return null;
                    }

                    @Override
                    public FlushModeType getFlushMode() {
                        return null;
                    }

                    @Override
                    public EntityManagerFactory getEntityManagerFactory() {
                        return null;
                    }

                    @Override
                    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
                        return null;
                    }

                    @Override
                    public EntityGraph<?> getEntityGraph(String graphName) {
                        return null;
                    }

                    @Override
                    public Object getDelegate() {
                        return null;
                    }

                    @Override
                    public CriteriaBuilder getCriteriaBuilder() {
                        return null;
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
                        return null;
                    }

                    @Override
                    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
                        return null;
                    }

                    @Override
                    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
                        return null;
                    }

                    @Override
                    public <T> T find(Class<T> entityClass, Object primaryKey) {
                        return null;
                    }

                    @Override
                    public void detach(Object entity) {
                    }

                    @Override
                    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
                        return null;
                    }

                    @Override
                    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
                        return null;
                    }

                    @Override
                    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
                        return null;
                    }

                    @Override
                    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
                        return null;
                    }

                    @Override
                    public Query createQuery(CriteriaDelete deleteQuery) {
                        return null;
                    }

                    @Override
                    public Query createQuery(CriteriaUpdate updateQuery) {
                        return null;
                    }

                    @Override
                    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
                        return null;
                    }

                    @Override
                    public Query createQuery(String qlString) {
                        return null;
                    }

                    @Override
                    public Query createNativeQuery(String sqlString, String resultSetMapping) {
                        return null;
                    }

                    @Override
                    public Query createNativeQuery(String sqlString, Class resultClass) {
                        return null;
                    }

                    @Override
                    public Query createNativeQuery(String sqlString) {
                        return null;
                    }

                    @Override
                    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
                        return null;
                    }

                    @Override
                    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
                        return null;
                    }

                    @Override
                    public Query createNamedQuery(String name) {
                        return null;
                    }

                    @Override
                    public EntityGraph<?> createEntityGraph(String graphName) {
                        return null;
                    }

                    @Override
                    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
                        return null;
                    }

                    @Override
                    public boolean contains(Object entity) {
                        return false;
                    }

                    @Override
                    public void close() {
                    }

                    @Override
                    public void clear() {
                    }
                };
                return () -> entityManager;
            }
            return null;
        }

    }
}
