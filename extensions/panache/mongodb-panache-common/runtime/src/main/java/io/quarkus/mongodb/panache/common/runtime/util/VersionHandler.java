package io.quarkus.mongodb.panache.common.runtime.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;

import io.quarkus.mongodb.panache.common.Version;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;

/**
 * Handle @Version in queries.
 **/
public class VersionHandler {

    /**
     * Factory method that build the query for updates and also adjust the versionValue when applicable.
     * Otherwise it does nothing
     */
    public static EntityVersionInfo buildAndAdjustEntityVersionInfo(Object entity, BsonDocument document) {
        EntityVersionInfo entityVersionInfo = buildEntityVersionInfo(entity, document);
        incrementVersionValue(entityVersionInfo);
        return entityVersionInfo;
    }

    /**
     * Factory method that build the query for updates and also adjust the versionValue when applicable.
     * Otherwise it does nothing
     */
    public static EntityVersionInfo buildEntityVersionInfo(Object entity, BsonDocument document) {
        Optional<Field> versionFieldOptional = extractVersionField(entity);

        BsonValue entityId = getEntityId(document);

        if (versionFieldOptional.isEmpty()) {
            return EntityVersionInfo.of(
                    entity,
                    entityId,
                    buildUpdateQuery(entityId, null, document));
        }

        Field versionField = versionFieldOptional.get();
        Long versionValueBeforeAdjust = extractVersionValue(versionField, entity);
        String versionFieldName = versionField.getName();
        BsonDocument updateQuery = buildUpdateQuery(entityId, versionFieldName, document);

        return EntityVersionInfo.of(
                entity,
                entityId,
                updateQuery,
                versionField,
                versionValueBeforeAdjust,
                versionFieldName);
    }

    private static Optional<Field> extractVersionField(Object entity) {
        List<Field> versionList = getAllVersionFields(entity.getClass());

        if (versionList.isEmpty()) {
            return Optional.empty();
        }

        if (versionList.size() > 1) {
            throw new IllegalArgumentException("Wrong mapped version, found more than 1 field annotated with @Version");
        }

        return Optional.of(versionList.get(0));
    }

    private static List<Field> getAllVersionFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();

        Field[] fields = clazz.getFields();
        Field[] declaredFields = clazz.getDeclaredFields();
        Stream.of(fields, declaredFields)
                .flatMap(Stream::of)
                .filter(field -> field.isAnnotationPresent(Version.class))
                .distinct()
                .collect(Collectors.toCollection(() -> fieldList));

        if (clazz.getSuperclass() != null) {
            fieldList.addAll(getAllVersionFields(clazz.getSuperclass()));
        }

        return fieldList.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private static Long extractVersionValue(Field versionField, Object entity) {
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

    public static void incrementVersionValue(EntityVersionInfo entityVersionInfo) {
        try {
            if (!entityVersionInfo.hasVersionAnnotation) {
                return;
            }

            Field versionField = entityVersionInfo.versionField;
            Object entity = entityVersionInfo.entity;

            boolean canAccess = versionField.canAccess(entity);
            versionField.setAccessible(true);

            Long versionValue = (Long) versionField.get(entity);
            versionField.set(entity, versionValue == null ? 0L : versionValue + 1);

            versionField.setAccessible(canAccess);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on set the version value");
        }
    }

    /**
     * Restore previous version value in the entity.
     */
    public static void rollbackVersion(EntityVersionInfo entityVersionInfo) {
        try {
            if (!entityVersionInfo.hasVersionAnnotation) {
                return;
            }

            Field versionField = entityVersionInfo.versionField;
            Object entity = entityVersionInfo.entity;
            boolean canAccess = versionField.canAccess(entity);
            versionField.setAccessible(true);

            versionField.set(entity, entityVersionInfo.versionValue);
            versionField.setAccessible(canAccess);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException("Error on set the version value: " + ex.getMessage());
        }
    }

    /**
     * Restore previous version value in the entity.
     */
    public static void resetVersionValueAndThrowOptimistic(EntityVersionInfo entityVersionInfo) {
        rollbackVersion(entityVersionInfo);
        throwOptimisticLockException(entityVersionInfo);
    }

    /**
     * Utility method to check if the during an update there are documents not updated.
     * Throws OptimisticLockException when found documents not updated.
     */
    public static void validateChanges(final EntityVersionInfo entityVersionInfo,
            Long quantityResultAffected) {
        if (entityVersionInfo.hasVersionAnnotation && quantityResultAffected == 0) {
            rollbackVersion(entityVersionInfo);
            throwOptimisticLockException(entityVersionInfo);
        }
    }

    /**
     * Build a message and throw a OptimisticLockException.
     */
    private static void throwOptimisticLockException(Object entity) {
        StringBuilder errorMsg = new StringBuilder("Was not possible to update entity: ")
                .append(entity.toString());
        throw new OptimisticLockException(errorMsg.toString());
    }

    /**
     * We build the query that will be used to update the document, can have id, or id/version.
     */
    private static BsonDocument buildUpdateQuery(BsonValue id,
            String versionFieldName,
            BsonDocument document) {
        //then we get its id field and create a new Document with only this one that will be our replace query
        if (id == null) {
            return null;
        }

        if (versionFieldName != null) {
            BsonValue version = document.get(versionFieldName);
            if (version == null) {
                version = new BsonNull();
            }
            return new BsonDocument().append(MongoOperations.ID, id).append(versionFieldName, version);
        }
        return new BsonDocument().append(MongoOperations.ID, id);
    }

    private static BsonValue getEntityId(final BsonDocument document) {
        return document.get(MongoOperations.ID);
    }
}
