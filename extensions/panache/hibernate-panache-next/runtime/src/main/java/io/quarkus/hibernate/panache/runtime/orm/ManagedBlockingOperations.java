package io.quarkus.hibernate.panache.runtime.orm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.Session;
import org.hibernate.StatelessSession;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.panache.common.Sort;

public class ManagedBlockingOperations implements PanacheBlockingOperations {

    public final static ManagedBlockingOperations INSTANCE = new ManagedBlockingOperations();
    private final static ManagedBlockingJpaOperations DELEGATE = new ManagedBlockingJpaOperations();

    private ManagedBlockingOperations() {
    }

    @Override
    public Session getSession(Class<?> entityClass) {
        return DELEGATE.getSession(entityClass);
    }

    @Override
    public StatelessSession getStatelessSession(Class<?> entityClass) {
        // FIXME: this is wrong
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Void insert(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Void persist(Object entity) {
        DELEGATE.persist(entity);
        return null;
    }

    @Override
    public Void persistAndFlush(Object entity) {
        DELEGATE.persist(entity);
        DELEGATE.flush(entity);
        return null;
    }

    @Override
    public Void delete(Object entity) {
        DELEGATE.delete(entity);
        return null;
    }

    @Override
    public Void update(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Void upsert(Object entity) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Boolean isPersistent(Object entity) {
        return DELEGATE.isPersistent(entity);
    }

    @Override
    public Void flush(Object entity) {
        DELEGATE.flush(entity);
        return null;
    }

    @Override
    public Void persist(Iterable<Object> entities) {
        DELEGATE.persist(entities);
        return null;
    }

    @Override
    public Void persist(Stream<Object> entities) {
        DELEGATE.persist(entities);
        return null;
    }

    @Override
    public Void persist(Object firstEntity, Object... entities) {
        DELEGATE.persist(firstEntity, entities);
        return null;
    }

    @Override
    public Void insert(Iterable<Object> entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Void insert(Stream<Object> entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Void insert(Object firstEntity, Object... entities) {
        throw new UnsupportedOperationException("Stateless operations not supported");
    }

    @Override
    public Object findById(Class<?> entityClass, Object id) {
        return DELEGATE.findById(entityClass, id);
    }

    @Override
    public Object findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return DELEGATE.findById(entityClass, id, lockModeType);
    }

    @Override
    public PanacheBlockingQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.find(entityClass, query, params);
    }

    @Override
    public PanacheBlockingQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        return DELEGATE.find(entityClass, query, sort, params);
    }

    @Override
    public PanacheBlockingQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.find(entityClass, query, params);
    }

    @Override
    public PanacheBlockingQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return DELEGATE.find(entityClass, query, sort, params);
    }

    @Override
    public PanacheBlockingQuery<?> findAll(Class<?> entityClass) {
        return DELEGATE.findAll(entityClass);
    }

    @Override
    public PanacheBlockingQuery<?> findAll(Class<?> entityClass, Sort sort) {
        return DELEGATE.findAll(entityClass, sort);
    }

    @Override
    public List<?> list(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.list(entityClass, query, params);
    }

    @Override
    public List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return DELEGATE.list(entityClass, query, sort, params);
    }

    @Override
    public List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.list(entityClass, query, params);
    }

    @Override
    public List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return DELEGATE.list(entityClass, query, sort, params);
    }

    @Override
    public List<?> listAll(Class<?> entityClass) {
        return DELEGATE.listAll(entityClass);
    }

    @Override
    public List<?> listAll(Class<?> entityClass, Sort sort) {
        return DELEGATE.listAll(entityClass, sort);
    }

    @Override
    public Long count(Class<?> entityClass) {
        return DELEGATE.count(entityClass);
    }

    @Override
    public Long count(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.count(entityClass, query, params);
    }

    @Override
    public Long count(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.count(entityClass, query, params);
    }

    @Override
    public Long deleteAll(Class<?> entityClass) {
        return DELEGATE.deleteAll(entityClass);
    }

    @Override
    public Boolean deleteById(Class<?> entityClass, Object id) {
        return DELEGATE.deleteById(entityClass, id);
    }

    @Override
    public Long delete(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.delete(entityClass, query, params);
    }

    @Override
    public Long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.delete(entityClass, query, params);
    }

    @Override
    public Long update(Class<?> entityClass, String query, Object... params) {
        return (long) DELEGATE.update(entityClass, query, params);
    }

    @Override
    public Long update(Class<?> entityClass, String query, Map<String, Object> params) {
        return (long) DELEGATE.update(entityClass, query, params);
    }

    @Override
    public Optional<?> findByIdOptional(Class<?> entityClass, Object id) {
        return DELEGATE.findByIdOptional(entityClass, id);
    }

    @Override
    public Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return DELEGATE.findByIdOptional(entityClass, id, lockModeType);
    }

    @Override
    public Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return DELEGATE.stream(entityClass, query, params);
    }

    @Override
    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return DELEGATE.stream(entityClass, query, sort, params);
    }

    @Override
    public Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return DELEGATE.stream(entityClass, query, params);
    }

    @Override
    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return DELEGATE.stream(entityClass, query, sort, params);
    }

    @Override
    public Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return DELEGATE.streamAll(entityClass, sort);
    }

    @Override
    public Stream<?> streamAll(Class<?> entityClass) {
        return DELEGATE.streamAll(entityClass);
    }
}
