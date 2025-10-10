package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public abstract class AbstractManagedJpaOperations<PanacheQueryType>
        extends AbstractJpaOperations<PanacheQueryType, Mutiny.Session> {
    protected AbstractManagedJpaOperations() {
        super(Mutiny.Session.class);
    }

    public Uni<Void> persist(Object entity) {
        return persist(getSession(), entity);
    }

    public Uni<Void> persist(Uni<Mutiny.Session> sessionUni, Object entity) {
        return sessionUni.chain(session -> {
            if (!session.contains(entity)) {
                return session.persist(entity);
            }
            return Uni.createFrom().nullItem();
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Void> persist(Iterable<?> entities) {
        List list = new ArrayList();
        for (Object entity : entities) {
            list.add(entity);
        }
        return persist(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> persist(Object firstEntity, Object... entities) {
        List<Object> list = new ArrayList<>(entities.length + 1);
        list.add(firstEntity);
        Collections.addAll(list, entities);
        return persist(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> persist(Stream<?> entities) {
        return persist(entities.toArray());
    }

    public Uni<Void> persist(Object... entities) {
        return getSession().chain(session -> session.persistAll(entities));
    }

    public Uni<Void> delete(Object entity) {
        return getSession().chain(session -> session.remove(entity));
    }

    @Override
    public Uni<Void> delete(Mutiny.Session session, Object entity) {
        return session.remove(entity);
    }

    @Override
    protected <T> Uni<T> find(Mutiny.Session session, Class<T> entityClass, Object id) {
        return session.find(entityClass, id);
    }

    @Override
    protected <T> Uni<T> find(Mutiny.Session session, Class<T> entityClass, Object id, LockMode lockMode) {
        return session.find(entityClass, id, lockMode);
    }

    @Override
    protected <R> Mutiny.SelectionQuery<R> createSelectionQuery(Mutiny.Session session, String var1, Class<R> var2) {
        return session.createSelectionQuery(var1, var2);
    }

    @Override
    protected <R> Mutiny.SelectionQuery<R> createNamedQuery(Mutiny.Session session, String var1, Class<R> var2) {
        return session.createNamedQuery(var1, var2);
    }

    @Override
    protected <R> Mutiny.Query<R> createNamedQuery(Mutiny.Session session, String var1) {
        return session.createNamedQuery(var1);
    }

    @Override
    protected Mutiny.MutationQuery createMutationQuery(Mutiny.Session session, String var1) {
        return session.createMutationQuery(var1);
    }

    public boolean isPersistent(Object entity) {
        Mutiny.Session current = SessionOperations.getCurrentSession();
        return current != null ? current.contains(entity) : false;
    }

    public Uni<Void> flush() {
        return getSession().chain(Mutiny.Session::flush);
    }

}
