package io.quarkus.hibernate.panache.runtime.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.persistence.LockModeType;

import org.hibernate.Session;
import org.hibernate.StatelessSession;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;

public interface PanacheBlockingOperations extends
        PanacheOperations<Object, List<?>, PanacheBlockingQuery<?>, Long, Void, Boolean> {

    Session getSession(Class<?> entityClass);

    StatelessSession getStatelessSession(Class<?> entityClass);

    Optional<?> findByIdOptional(Class<?> entityClass, Object id);

    Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType);

    Stream<?> stream(Class<?> entityClass, String query, Object... params);

    Stream<?> stream(Class<?> entityClass, String query, Order<?> order, Object... params);

    Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params);

    Stream<?> stream(Class<?> entityClass, String query, Order<?> order, Map<String, Object> params);

    Stream<?> streamAll(Class<?> entityClass, Order<?> order);

    Stream<?> streamAll(Class<?> entityClass);
}
