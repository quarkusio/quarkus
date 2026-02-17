package io.quarkus.hibernate.panache.runtime.spi;

import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import io.quarkus.hibernate.panache.runtime.hr.ManagedReactiveOperations;
import io.quarkus.hibernate.panache.runtime.hr.StatelessReactiveOperations;
import io.quarkus.hibernate.panache.runtime.orm.ManagedBlockingOperations;
import io.quarkus.hibernate.panache.runtime.orm.StatelessBlockingOperations;
import io.quarkus.panache.common.Sort;

public interface PanacheOperations<One, Many, Query, Count, Completion, Confirmation, Session, StatelessSession> {

    static PanacheBlockingOperations getBlockingManaged() {
        return ManagedBlockingOperations.INSTANCE;
    }

    static PanacheReactiveOperations getReactiveManaged() {
        return ManagedReactiveOperations.INSTANCE;
    }

    static PanacheBlockingOperations getBlockingStateless() {
        return StatelessBlockingOperations.INSTANCE;
    }

    static PanacheReactiveOperations getReactiveStateless() {
        return StatelessReactiveOperations.INSTANCE;
    }

    // Operations

    Session getSession(Class<?> entityClass);

    StatelessSession getStatelessSession(Class<?> entityClass);

    Completion insert(Object entity);

    Completion persist(Object entity);

    Completion persistAndFlush(Object entity);

    Completion delete(Object entity);

    Completion update(Object entity);

    Completion upsert(Object entity);

    Confirmation isPersistent(Object entity);

    Completion flush(Object entity);

    Completion persist(Iterable<Object> entities);

    Completion persist(Stream<Object> entities);

    Completion persist(Object firstEntity, Object... entities);

    Completion insert(Iterable<Object> entities);

    Completion insert(Stream<Object> entities);

    Completion insert(Object firstEntity, Object... entities);

    // Queries

    One findById(Class<?> entityClass, Object id);

    One findById(Class<?> entityClass, Object id, LockModeType lockModeType);

    Query find(Class<?> entityClass, String query, Object... params);

    Query find(Class<?> entityClass, String query, Sort sort, Object... params);

    Query find(Class<?> entityClass, String query, Map<String, Object> params);

    Query find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params);

    Query findAll(Class<?> entityClass);

    Query findAll(Class<?> entityClass, Sort sort);

    Many list(Class<?> entityClass, String query, Object... params);

    Many list(Class<?> entityClass, String query, Sort sort, Object... params);

    Many list(Class<?> entityClass, String query, Map<String, Object> params);

    Many list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params);

    Many listAll(Class<?> entityClass);

    Many listAll(Class<?> entityClass, Sort sort);

    Count count(Class<?> entityClass);

    Count count(Class<?> entityClass, String query, Object... params);

    Count count(Class<?> entityClass, String query, Map<String, Object> params);

    Count deleteAll(Class<?> entityClass);

    Confirmation deleteById(Class<?> entityClass, Object id);

    Count delete(Class<?> entityClass, String query, Object... params);

    Count delete(Class<?> entityClass, String query, Map<String, Object> params);

    Count update(Class<?> entityClass, String query, Object... params);

    Count update(Class<?> entityClass, String query, Map<String, Object> params);
}
