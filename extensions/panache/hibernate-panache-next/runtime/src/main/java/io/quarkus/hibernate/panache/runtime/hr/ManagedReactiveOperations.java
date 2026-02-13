package io.quarkus.hibernate.panache.runtime.hr;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.panache.reactive.PanacheReactiveQuery;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class ManagedReactiveOperations implements PanacheReactiveOperations {

    public final static ManagedReactiveOperations INSTANCE = new ManagedReactiveOperations();
    private final static ManagedReactiveJpaOperations DELEGATE = new ManagedReactiveJpaOperations();

    private ManagedReactiveOperations() {
    }

    @Override
    public Uni<Mutiny.Session> getSession(Class<?> entityClass) {
        return DELEGATE.getSession(entityClass);
    }

    @Override
    public Uni<Mutiny.StatelessSession> getStatelessSession(Class<?> entityClass) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Void> insert(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Void> persist(Object entity) {
        return DELEGATE.persist(entity);
    }

    @Override
    public Uni<Void> persistAndFlush(Object entity) {
        return DELEGATE.persist(entity)
                .chain(v -> DELEGATE.flush(entity.getClass()));
    }

    @Override
    public Uni<Void> delete(Object entity) {
        return DELEGATE.delete(entity);
    }

    @Override
    public Uni<Void> update(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Void> upsert(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Boolean> isPersistent(Object entity) {
        return Uni.createFrom().item(DELEGATE.isPersistent(entity));
    }

    @Override
    public Uni<Void> flush(Object entity) {
        return DELEGATE.flush(entity);
    }

    @Override
    public Uni<Void> persist(Iterable<Object> entities) {
        return DELEGATE.persist(entities);
    }

    @Override
    public Uni<Void> persist(Stream<Object> entities) {
        return DELEGATE.persist(entities);
    }

    @Override
    public Uni<Void> persist(Object firstEntity, Object... entities) {
        return DELEGATE.persist(firstEntity, entities);
    }

    @Override
    public Uni<Void> insert(Iterable<Object> entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Void> insert(Stream<Object> entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Void> insert(Object firstEntity, Object... entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Uni<Object> findById(Class<?> entityClass, Object id) {
        return (Uni) DELEGATE.findById(entityClass, id);
    }

    @Override
    public Uni<Object> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return (Uni) DELEGATE.findById(entityClass, id, lockModeType);
    }

    @Override
    public PanacheReactiveQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.find(entityClass, query, params);
    }

    @Override
    public PanacheReactiveQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        return DELEGATE.find(entityClass, query, sort, params);
    }

    @Override
    public PanacheReactiveQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.find(entityClass, query, params);
    }

    @Override
    public PanacheReactiveQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return DELEGATE.find(entityClass, query, sort, params);
    }

    @Override
    public PanacheReactiveQuery<?> findAll(Class<?> entityClass) {
        return DELEGATE.findAll(entityClass);
    }

    @Override
    public PanacheReactiveQuery<?> findAll(Class<?> entityClass, Sort sort) {
        return DELEGATE.findAll(entityClass, sort);
    }

    @Override
    public Uni<List<?>> list(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.list(entityClass, query, params);
    }

    @Override
    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return DELEGATE.list(entityClass, query, sort, params);
    }

    @Override
    public Uni<List<?>> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.list(entityClass, query, params);
    }

    @Override
    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return DELEGATE.list(entityClass, query, sort, params);
    }

    @Override
    public Uni<List<?>> listAll(Class<?> entityClass) {
        return DELEGATE.listAll(entityClass);
    }

    @Override
    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return DELEGATE.listAll(entityClass, sort);
    }

    @Override
    public Uni<Long> count(Class<?> entityClass) {
        return DELEGATE.count(entityClass);
    }

    @Override
    public Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.count(entityClass, query, params);
    }

    @Override
    public Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.count(entityClass, query, params);
    }

    @Override
    public Uni<Long> deleteAll(Class<?> entityClass) {
        return DELEGATE.deleteAll(entityClass);
    }

    @Override
    public Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        return DELEGATE.deleteById(entityClass, id);
    }

    @Override
    public Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.delete(entityClass, query, params);
    }

    @Override
    public Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.delete(entityClass, query, params);
    }

    @Override
    public Uni<Long> update(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.update(entityClass, query, params).map(i -> i.longValue());
    }

    @Override
    public Uni<Long> update(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.update(entityClass, query, params).map(i -> i.longValue());
    }
}
