package io.quarkus.spring.data.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public final class RepositorySupport {

    private RepositorySupport() {
    }

    public static List<?> findByIds(Class<?> entityClass, Iterable<?> ids) {
        Objects.requireNonNull(ids);
        List<Object> result = new ArrayList<>();
        for (Object id : ids) {
            Object byId = JpaOperations.findById(entityClass, id);
            if (byId != null) {
                result.add(byId);
            }
        }
        return result;
    }

    public static List<?> findByIds(Class<?> entityClass, String idField, Iterable<Long> ids) {
        Objects.requireNonNull(ids);
        return JpaOperations.find(entityClass, String.format("%s in ?1", idField), ids).list();
    }

    public static void deleteAll(Iterable<?> entities) {
        for (Object entity : entities) {
            JpaOperations.delete(entity);
        }
    }

    public static Object getOne(Class<?> entityClass, Object id) {
        return JpaOperations.getEntityManager().getReference(entityClass, id);
    }
}
