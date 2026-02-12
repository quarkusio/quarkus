package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.StatelessSession;

public abstract class AbstractStatelessJpaOperations<PanacheQueryType>
        extends AbstractJpaOperations<PanacheQueryType, StatelessSession> {

    protected AbstractStatelessJpaOperations() {
        super(StatelessSession.class);
    }

    public void insert(Object entity) {
        StatelessSession session = getSession(entity.getClass());
        insert(session, entity);
    }

    public void insert(StatelessSession session, Object entity) {
        session.insert(entity);
    }

    public void insert(Iterable<?> entities) {
        for (Object entity : entities) {
            insert(getSession(entity.getClass()), entity);
        }
    }

    public void insert(Object firstEntity, Object... entities) {
        insert(firstEntity);
        for (Object entity : entities) {
            insert(entity);
        }
    }

    public void insert(Stream<?> entities) {
        entities.forEach(entity -> insert(entity));
    }

    public void update(Object entity) {
        StatelessSession session = getSession(entity.getClass());
        update(session, entity);
    }

    public void update(StatelessSession session, Object entity) {
        session.update(entity);
    }

    public void upsert(Object entity) {
        StatelessSession session = getSession(entity.getClass());
        upsert(session, entity);
    }

    public void upsert(StatelessSession session, Object entity) {
        session.upsert(entity);
    }

    public void delete(Object entity) {
        StatelessSession session = getSession(entity.getClass());
        session.delete(entity);
    }

    // Query methods

    public <T> List<T> findByIds(Class<T> klass, List<?> ids) {
        return getSession(klass).getMultiple(klass, ids);
    }

    public Object findById(Class<?> entityClass, Object id) {
        return getSession(entityClass).get(entityClass, id);
    }

    public Object findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession(entityClass).get(entityClass, id, LockMode.fromJpaLockMode(lockModeType));
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return Optional.ofNullable(findById(entityClass, id, lockModeType));
    }

    public boolean deleteById(Class<?> entityClass, Object id) {
        // FIXME: not sure this is true for stateless sessions, ask Yoann
        // Impl note : we load the entity then delete it because it's the only implementation generic enough for any model,
        // and correct in all cases (composite key, graph of entities, ...). HQL cannot be directly used for these reasons.
        Object entity = findById(entityClass, id);
        if (entity == null) {
            return false;
        }
        getSession(entityClass).delete(entity);
        return true;
    }
}
