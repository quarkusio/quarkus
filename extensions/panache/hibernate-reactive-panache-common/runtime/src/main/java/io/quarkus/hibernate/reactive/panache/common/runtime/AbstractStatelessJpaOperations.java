package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public abstract class AbstractStatelessJpaOperations<PanacheQueryType>
        extends AbstractJpaOperations<PanacheQueryType, Mutiny.StatelessSession> {
    protected AbstractStatelessJpaOperations() {
        super(Mutiny.StatelessSession.class);
    }

    public Uni<Void> insert(Object entity) {
        return insert(getSession(entity.getClass()), entity);
    }

    public Uni<Void> insert(Uni<Mutiny.StatelessSession> sessionUni, Object entity) {
        return sessionUni.chain(session -> session.insert(entity));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Void> insert(Iterable<?> entities) {
        List list = new ArrayList();
        for (Object entity : entities) {
            list.add(entity);
        }
        return insert(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> insert(Object firstEntity, Object... entities) {
        List<Object> list = new ArrayList<>(entities.length + 1);
        list.add(firstEntity);
        Collections.addAll(list, entities);
        return insert(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> insert(Stream<?> entities) {
        return insert(entities.toArray());
    }

    public Uni<Void> insert(Object... entities) {
        Map<String, List<Object>> sessions = Arrays.stream(entities)
                .collect(Collectors.groupingBy(e -> entityToPersistenceUnit.get(e.getClass().getName())));

        List<Uni<Void>> results = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : sessions.entrySet()) {
            results.add(getSession(entry.getKey()).chain(session -> session.insertAll(entry.getValue().toArray())));
        }

        return Uni.combine().all().unis(results).discardItems();
    }

    public Uni<Void> update(Object entity) {
        return getSession(entity.getClass()).chain(session -> session.update(entity));
    }

    public Uni<Void> upsert(Object entity) {
        return getSession(entity.getClass()).chain(session -> session.upsert(entity));
    }

    public Uni<Void> delete(Object entity) {
        return getSession(entity.getClass()).chain(session -> session.delete(entity));
    }

    @Override
    public Uni<Void> delete(Mutiny.StatelessSession session, Object entity) {
        return session.delete(entity);
    }

    @Override
    protected <T> Uni<T> find(Mutiny.StatelessSession session, Class<T> entityClass, Object id) {
        return session.get(entityClass, id);
    }

    @Override
    protected <T> Uni<T> find(Mutiny.StatelessSession session, Class<T> entityClass, Object id, LockMode lockMode) {
        return session.get(entityClass, id, lockMode);
    }

    @Override
    protected <R> Mutiny.SelectionQuery<R> createSelectionQuery(Mutiny.StatelessSession session, String var1, Class<R> var2) {
        return session.createSelectionQuery(var1, var2);
    }

    @Override
    protected <R> Mutiny.SelectionQuery<R> createNamedQuery(Mutiny.StatelessSession session, String var1, Class<R> var2) {
        return session.createNamedQuery(var1, var2);
    }

    @Override
    protected <R> Mutiny.Query<R> createNamedQuery(Mutiny.StatelessSession session, String var1) {
        return session.createNamedQuery(var1);
    }

    @Override
    protected Mutiny.MutationQuery createMutationQuery(Mutiny.StatelessSession session, String var1) {
        return session.createMutationQuery(var1);
    }
}
