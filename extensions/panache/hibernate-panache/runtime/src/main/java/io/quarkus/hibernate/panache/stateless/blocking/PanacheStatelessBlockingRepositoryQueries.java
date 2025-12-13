package io.quarkus.hibernate.panache.stateless.blocking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.hibernate.panache.blocking.PanacheRepositoryBlockingQueries;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.panache.common.Sort;

public interface PanacheStatelessBlockingRepositoryQueries<Entity, Id> extends PanacheRepositoryBlockingQueries<Entity, Id> {
    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
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
    default PanacheBlockingQuery<Entity> find(String query, Object... params) {
        return (PanacheBlockingQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default PanacheBlockingQuery<Entity> find(String query, Sort sort, Object... params) {
        return (PanacheBlockingQuery<Entity>) operations().find(getEntityClass(), query, sort, params);
    }

    @Override
    default PanacheBlockingQuery<Entity> find(String query, Map<String, Object> params) {
        return (PanacheBlockingQuery<Entity>) operations().find(getEntityClass(), query, params);
    }

    @Override
    default PanacheBlockingQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        return (PanacheBlockingQuery<Entity>) operations().find(getEntityClass(), query, sort, params);
    }

    @Override
    default PanacheBlockingQuery<Entity> findAll() {
        return (PanacheBlockingQuery<Entity>) operations().findAll(getEntityClass());
    }

    @Override
    default PanacheBlockingQuery<Entity> findAll(Sort sort) {
        return (PanacheBlockingQuery<Entity>) operations().findAll(getEntityClass(), sort);
    }

    @Override
    default List<Entity> list(String query, Object... params) {
        return (List<Entity>) operations().list(getEntityClass(), query, params);
    }

    @Override
    default List<Entity> list(String query, Sort sort, Object... params) {
        return (List<Entity>) operations().list(getEntityClass(), query, sort, params);
    }

    @Override
    default List<Entity> list(String query, Map<String, Object> params) {
        return (List<Entity>) operations().list(getEntityClass(), query, params);
    }

    @Override
    default List<Entity> list(String query, Sort sort, Map<String, Object> params) {
        return (List<Entity>) operations().list(getEntityClass(), query, sort, params);
    }

    @Override
    default List<Entity> listAll() {
        return (List<Entity>) operations().listAll(getEntityClass());
    }

    @Override
    default List<Entity> listAll(Sort sort) {
        return (List<Entity>) operations().listAll(getEntityClass(), sort);
    }

    @Override
    default Stream<Entity> stream(String query, Object... params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, params);
    }

    @Override
    default Stream<Entity> stream(String query, Sort sort, Object... params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, sort, params);
    }

    @Override
    default Stream<Entity> stream(String query, Map<String, Object> params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, params);
    }

    @Override
    default Stream<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        return (Stream<Entity>) operations().stream(getEntityClass(), query, sort, params);
    }

    @Override
    default Stream<Entity> streamAll(Sort sort) {
        return (Stream<Entity>) operations().streamAll(getEntityClass(), sort);
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
