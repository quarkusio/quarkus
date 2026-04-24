package io.quarkus.hibernate.panache.managed.reactive;

import java.util.List;
import java.util.Map;

import jakarta.data.Order;
import jakarta.persistence.LockModeType;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.reactive.PanacheReactiveQuery;
import io.quarkus.hibernate.panache.reactive.PanacheRepositoryReactiveQueries;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheManagedReactiveRepositoryQueries<Entity, Id> extends PanacheRepositoryReactiveQueries<Entity, Id> {
    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    @Override
    default Uni<Entity> findById(Id id) {
        return (Uni) operations().findById(getEntityClass(), id);
    }

    @Override
    default Uni<Entity> findById(Id id, LockModeType lockModeType) {
        return (Uni) operations().findById(getEntityClass(), id, lockModeType);
    }

    @Override
    default PanacheReactiveQuery<Entity> find(String query, Object... params) {
        return (PanacheReactiveQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default PanacheReactiveQuery<Entity> find(String query, Order<?> order, Object... params) {
        return (PanacheReactiveQuery<Entity>) operations().find(getEntityClass(), query, order, params);
    }

    @Override
    default PanacheReactiveQuery<Entity> find(String query, Map<String, Object> params) {
        return (PanacheReactiveQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default PanacheReactiveQuery<Entity> find(String query, Order<?> order, Map<String, Object> params) {
        return (PanacheReactiveQuery<Entity>) operations().find(getEntityClass(), query, order, params);
    }

    @Override
    default PanacheReactiveQuery<Entity> findAll() {
        return (PanacheReactiveQuery<Entity>) operations().findAll(getEntityClass());
    }

    @Override
    default PanacheReactiveQuery<Entity> findAll(Order<?> order) {
        return (PanacheReactiveQuery<Entity>) operations().findAll(getEntityClass(), order);
    }

    @Override
    default Uni<List<Entity>> list(String query, Object... params) {
        return (Uni) operations().list(getEntityClass(), query, params);
    }

    @Override
    default Uni<List<Entity>> list(String query, Order<?> order, Object... params) {
        return (Uni) operations().list(getEntityClass(), query, order, params);
    }

    @Override
    default Uni<List<Entity>> list(String query, Map<String, Object> params) {
        return (Uni) operations().list(getEntityClass(), query, params);
    }

    @Override
    default Uni<List<Entity>> list(String query, Order<?> order, Map<String, Object> params) {
        return (Uni) operations().list(getEntityClass(), query, order, params);
    }

    @Override
    default Uni<List<Entity>> listAll() {
        return (Uni) operations().listAll(getEntityClass());
    }

    @Override
    default Uni<List<Entity>> listAll(Order<?> order) {
        return (Uni) operations().listAll(getEntityClass(), order);
    }

    @Override
    default Uni<Long> count() {
        return operations().count(getEntityClass());
    }

    @Override
    default Uni<Long> count(String query, Object... params) {
        return operations().count(getEntityClass(), query, params);
    }

    @Override
    default Uni<Long> count(String query, Map<String, Object> params) {
        return operations().count(getEntityClass(), query, params);
    }

    @Override
    default Uni<Long> deleteAll() {
        return operations().deleteAll(getEntityClass());
    }

    @Override
    default Uni<Boolean> deleteById(Id id) {
        return operations().deleteById(getEntityClass(), id);
    }

    @Override
    default Uni<Long> delete(String query, Object... params) {
        return operations().delete(getEntityClass(), query, params);
    }

    @Override
    default Uni<Long> delete(String query, Map<String, Object> params) {
        return operations().delete(getEntityClass(), query, params);
    }

    @Override
    default Uni<Long> update(String query, Object... params) {
        return operations().update(getEntityClass(), query, params);
    }

    @Override
    default Uni<Long> update(String query, Map<String, Object> params) {
        return operations().update(getEntityClass(), query, params);
    }
}
