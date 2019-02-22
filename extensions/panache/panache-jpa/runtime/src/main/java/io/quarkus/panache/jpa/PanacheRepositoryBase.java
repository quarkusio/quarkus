package io.quarkus.panache.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.jpa.impl.JpaOperations;

public interface PanacheRepositoryBase<Entity, Id> {

    // Operations

    public default void persist(Entity entity) {
        JpaOperations.persist(entity);
    }

    public default void delete(Entity entity) {
        JpaOperations.delete(entity);
    }

    public default boolean isPersistent(Entity entity) {
        return JpaOperations.isPersistent(entity);
    }

    // Queries

    public default Entity findById(Id id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> find(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> findAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default PanacheQuery<Entity> findAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> list(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> listAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default List<Entity> listAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> stream(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> streamAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default Stream<Entity> streamAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long count() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long count(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long count(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long count(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long deleteAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long delete(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long delete(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default long delete(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public default void persist(Iterable<Entity> entities) {
        JpaOperations.persist(entities);
    }

    public default void persist(Stream<Entity> entities) {
        JpaOperations.persist(entities);
    }

    public default void persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        JpaOperations.persist(firstEntity, entities);
    }
}
