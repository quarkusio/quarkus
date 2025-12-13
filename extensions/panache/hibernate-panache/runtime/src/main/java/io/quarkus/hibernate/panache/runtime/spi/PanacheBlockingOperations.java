package io.quarkus.hibernate.panache.runtime.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.Session;
import org.hibernate.StatelessSession;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.panache.common.Sort;

public interface PanacheBlockingOperations extends
        PanacheOperations<Object, List<?>, PanacheBlockingQuery<?>, Long, Void, Boolean, Session, StatelessSession> {

    Optional<?> findByIdOptional(Class<?> entityClass, Object id);

    Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType);

    Stream<?> stream(Class<?> entityClass, String query, Object... params);

    Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params);

    Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params);

    Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params);

    Stream<?> streamAll(Class<?> entityClass, Sort sort);

    Stream<?> streamAll(Class<?> entityClass);
}
