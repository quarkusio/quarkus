package io.quarkus.panache.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.jpa.impl.JpaOperations;

public class PanacheEntityBase {

    // Operations

    public void persist() {
        JpaOperations.persist(this);
    }

    public void delete() {
        JpaOperations.delete(this);
    }

    public boolean isPersistent() {
        return JpaOperations.isPersistent(this);
    }

    // Queries

    public static <T extends PanacheEntityBase> T findById(Object id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> findAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> PanacheQuery<T> findAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> listAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> List<T> listAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> streamAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static <T extends PanacheEntityBase> Stream<T> streamAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long count() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long count(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long count(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long count(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long deleteAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long delete(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long delete(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static long delete(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    public static void persist(Iterable<?> entities) {
        JpaOperations.persist(entities);
    }

    public static void persist(Stream<?> entities) {
        JpaOperations.persist(entities);
    }

    public static void persist(Object firstEntity, Object... entities) {
        JpaOperations.persist(firstEntity, entities);
    }
}
