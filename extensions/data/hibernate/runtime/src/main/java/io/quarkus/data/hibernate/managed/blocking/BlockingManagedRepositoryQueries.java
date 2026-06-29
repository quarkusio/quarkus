package io.quarkus.data.hibernate.managed.blocking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.persistence.LockModeType;

import io.quarkus.data.hibernate.blocking.BlockingDataQuery;
import io.quarkus.data.hibernate.blocking.BlockingRepositoryQueries;
import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public interface BlockingManagedRepositoryQueries<Entity, Id> extends BlockingRepositoryQueries<Entity, Id> {
    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    @Override
    default Entity findById(Id id) {
        return (Entity) operations().findById(getEntityClass(), id);
    }

    @Override
    default Entity findById(Id id, LockModeType lockModeType) {
        return (Entity) operations().findById(getEntityClass(), id, lockModeType);
    }

    @Override
    default Optional<Entity> findByIdOptional(Id id) {
        return (Optional<Entity>) operations().findByIdOptional(getEntityClass(), id);
    }

    @Override
    default Optional<Entity> findByIdOptional(Id id, LockModeType lockModeType) {
        return (Optional<Entity>) operations().findByIdOptional(getEntityClass(), id, lockModeType);
    }

    @Override
    default BlockingDataQuery<Entity> find(String query, Object... params) {
        return (BlockingDataQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default BlockingDataQuery<Entity> find(String query, Order<?> order, Object... params) {
        return (BlockingDataQuery<Entity>) operations().find(getEntityClass(), query, order, params);
    }

    @Override
    default BlockingDataQuery<Entity> find(String query, Map<String, Object> params) {
        return (BlockingDataQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default BlockingDataQuery<Entity> find(String query, Order<?> order, Map<String, Object> params) {
        return (BlockingDataQuery<Entity>) operations().find(getEntityClass(), query, order, params);
    }

    @Override
    default BlockingDataQuery<Entity> findAll() {
        return (BlockingDataQuery<Entity>) operations().findAll(getEntityClass());
    }

    @Override
    default BlockingDataQuery<Entity> findAll(Order<?> order) {
        return (BlockingDataQuery<Entity>) operations().findAll(getEntityClass(), order);
    }

    @Override
    default List<Entity> list(String query, Object... params) {
        return (List<Entity>) operations().list(getEntityClass(), query, params);
    }

    @Override
    default List<Entity> list(String query, Order<?> order, Object... params) {
        return (List<Entity>) operations().list(getEntityClass(), query, order, params);
    }

    @Override
    default List<Entity> list(String query, Map<String, Object> params) {
        return (List<Entity>) operations().list(getEntityClass(), query, params);
    }

    @Override
    default List<Entity> list(String query, Order<?> order, Map<String, Object> params) {
        return (List<Entity>) operations().list(getEntityClass(), query, order, params);
    }

    @Override
    default List<Entity> listAll() {
        return (List<Entity>) operations().listAll(getEntityClass());
    }

    @Override
    default List<Entity> listAll(Order<?> order) {
        return (List<Entity>) operations().listAll(getEntityClass(), order);
    }

    @Override
    default Stream<Entity> stream(String query, Object... params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, params);
    }

    @Override
    default Stream<Entity> stream(String query, Order<?> order, Object... params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, order, params);
    }

    @Override
    default Stream<Entity> stream(String query, Map<String, Object> params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, params);
    }

    @Override
    default Stream<Entity> stream(String query, Order<?> order, Map<String, Object> params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, order, params);
    }

    @Override
    default Stream<Entity> streamAll(Order<?> order) {
        return (Stream<Entity>) operations().streamAll(getEntityClass(), order);
    }

    @Override
    default Stream<Entity> streamAll() {
        return (Stream<Entity>) operations().streamAll(getEntityClass());
    }

    @Override
    default Long count() {
        return operations().count(getEntityClass());
    }

    @Override
    default Long count(String query, Object... params) {
        return operations().count(getEntityClass(), query, params);
    }

    @Override
    default Long count(String query, Map<String, Object> params) {
        return operations().count(getEntityClass(), query, params);
    }

    @Override
    default Long deleteAll() {
        return operations().deleteAll(getEntityClass());
    }

    @Override
    default Boolean deleteById(Id id) {
        return operations().deleteById(getEntityClass(), id);
    }

    @Override
    default Long delete(String query, Object... params) {
        return operations().delete(getEntityClass(), query, params);
    }

    @Override
    default Long delete(String query, Map<String, Object> params) {
        return operations().delete(getEntityClass(), query, params);
    }

    @Override
    default Long update(String query, Object... params) {
        return operations().update(getEntityClass(), query, params);
    }

    @Override
    default Long update(String query, Map<String, Object> params) {
        return operations().update(getEntityClass(), query, params);
    }
}
