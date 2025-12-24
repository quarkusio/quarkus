package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.hibernate.Session;

public abstract class AbstractManagedJpaOperations<PanacheQueryType> extends AbstractJpaOperations<PanacheQueryType, Session> {

    protected AbstractManagedJpaOperations() {
        super(Session.class);
    }

    /**
     * Returns the {@link EntityManager} for the given {@link Class<?> entity}
     *
     * @return {@link EntityManager}
     */
    public EntityManager getEntityManager(Class<?> clazz) {
        return getSession(clazz);
    }

    // This is here for binary compatibility for callers who expect Session as a return type
    @Override
    public Session getSession(Class<?> clazz) {
        return super.getSession(clazz);
    }

    public void persist(Object entity) {
        Session session = getSession(entity.getClass());
        persist(session, entity);
    }

    public void persist(Session session, Object entity) {
        if (!session.contains(entity)) {
            session.persist(entity);
        }
    }

    public void persist(Iterable<?> entities) {
        for (Object entity : entities) {
            persist(getSession(entity.getClass()), entity);
        }
    }

    public void persist(Object firstEntity, Object... entities) {
        persist(firstEntity);
        for (Object entity : entities) {
            persist(entity);
        }
    }

    public void persist(Stream<?> entities) {
        entities.forEach(entity -> persist(entity));
    }

    public void delete(Object entity) {
        Session session = getSession(entity.getClass());
        session.remove(session.contains(entity) ? entity : session.getReference(entity));
    }

    public boolean isPersistent(Object entity) {
        return getSession(entity.getClass()).contains(entity);
    }

    public void flush() {
        getSession().flush();
    }

    public void flush(Object entity) {
        getSession(entity.getClass()).flush();
    }

    public void flush(Class<?> clazz) {
        getSession(clazz).flush();
    }

    // Query methods

    public <T> List<T> findByIds(Class<T> klass, List<?> ids) {
        return getSession(klass).findMultiple(klass, ids);
    }

    public Object findById(Class<?> entityClass, Object id) {
        return getSession(entityClass).find(entityClass, id);
    }

    public Object findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession(entityClass).find(entityClass, id, lockModeType);
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return Optional.ofNullable(findById(entityClass, id, lockModeType));
    }

    public boolean deleteById(Class<?> entityClass, Object id) {
        // Impl note : we load the entity then delete it because it's the only implementation generic enough for any model,
        // and correct in all cases (composite key, graph of entities, ...). HQL cannot be directly used for these reasons.
        Object entity = findById(entityClass, id);
        if (entity == null) {
            return false;
        }
        getSession(entityClass).remove(entity);
        return true;
    }
}
