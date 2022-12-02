package io.quarkus.spring.data.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public final class RepositorySupport {

    private RepositorySupport() {
    }

    public static List<?> findByIds(AbstractJpaOperations<PanacheQuery<?>> operations, Class<?> entityClass,
            Iterable<?> ids) {
        Objects.requireNonNull(ids);
        List<Object> result = new ArrayList<>();
        for (Object id : ids) {
            Object byId = operations.findById(entityClass, id);
            if (byId != null) {
                result.add(byId);
            }
        }
        return result;
    }

    public static List<?> findByIds(AbstractJpaOperations<PanacheQuery<?>> operations, Class<?> entityClass,
            String idField, Iterable<Long> ids) {
        Objects.requireNonNull(ids);
        return operations.find(entityClass, String.format("%s in ?1", idField), ids).list();
    }

    public static void deleteAll(AbstractJpaOperations<PanacheQuery<?>> operations, Iterable<?> entities) {
        for (Object entity : entities) {
            operations.delete(entity);
        }
    }

    public static Object getOne(AbstractJpaOperations<PanacheQuery<?>> operations, Class<?> entityClass, Object id) {
        return operations.getEntityManager().getReference(entityClass, id);
    }

    public static void clear(Class<?> clazz) {
        EntityManager em = Panache.getEntityManager(clazz);
        em.clear();
    }

    public static void flush(Class<?> clazz) {
        EntityManager em = Panache.getEntityManager(clazz);
        em.flush();
    }
}
