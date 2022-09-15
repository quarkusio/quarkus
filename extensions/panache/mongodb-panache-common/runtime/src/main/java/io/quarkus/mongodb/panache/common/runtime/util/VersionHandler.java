package io.quarkus.mongodb.panache.common.runtime.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import io.quarkus.mongodb.panache.common.Version;

/**
 * Handle @Version in queries.
 **/
public class VersionHandler {

    public final Object entity;
    public final Field versionField;
    public final Long versionValue;

    private VersionHandler(Object entity, Field versionField, Long versionValue) {
        this.entity = entity;
        this.versionField = versionField;
        this.versionValue = versionValue;
    }

    public static VersionHandler of(Object entity) {
        Field versionField = extractVersionField(entity);
        Long versionValue = extractVersionValue(versionField, entity);

        return new VersionHandler(entity, versionField, versionValue);
    }

    public boolean containsVersionAnnotation() {
        return versionField != null;
    }

    public boolean containsVersionValue() {
        return versionValue != null;
    }

    public boolean containsVersionAnnotationAndValue() {
        return containsVersionAnnotation() && containsVersionValue();
    }

    private static Field extractVersionField(Object entity) {
        Field[] fields = entity.getClass().getFields();
        Field[] declaredFields = entity.getClass().getDeclaredFields();
        return Stream.of(fields, declaredFields)
                .flatMap(Stream::of)
                .filter(field -> field.isAnnotationPresent(Version.class))
                .findFirst().orElse(null);
    }

    public static Long extractVersionValue(Field versionField, Object entity) {
        try {
            if (versionField == null) {
                return null;
            }

            boolean canAccess = versionField.canAccess(entity);
            versionField.setAccessible(true);
            Long versionValue = (Long) versionField.get(entity);
            versionField.setAccessible(canAccess);
            return versionValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on set the version value");
        }
    }

    public void adjustVersionValue() {
        try {
            if (!containsVersionAnnotation()) {
                return;
            }

            boolean canAccess = versionField.canAccess(entity);
            versionField.setAccessible(true);

            Long versionValue = (Long) versionField.get(entity);
            versionField.set(entity, versionValue == null ? 0l : versionValue + 1);

            versionField.setAccessible(canAccess);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on set the version value");
        }
    }
}
