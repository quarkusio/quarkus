package io.quarkus.mongodb.panache.common.runtime.util;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;

import io.quarkus.mongodb.panache.common.Version;
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;

/**
 * Handle @Version in queries.
 **/
public class VersionHandler {

    public final Object entity;
    public final Field versionField;
    public final BsonDocument query;
    public final Boolean containsVersionAnnotation;
    public final Boolean containsVersionValue;
    public final String versionFieldName;

    private VersionHandler(final Object entity,
            final BsonDocument query,
            Field versionField,
            Boolean containsVersionAnnotation,
            Boolean containsVersionValue,
            String versionFieldName) {
        this.entity = entity;
        this.query = query;
        this.versionField = versionField;
        this.containsVersionAnnotation = containsVersionAnnotation;
        this.containsVersionValue = containsVersionValue;
        this.versionFieldName = versionFieldName;

        if (versionField != null) {
            adjustVersionValue();
        }
    }

    public static VersionHandler of(Object entity, BsonDocument document) {
        Optional<Field> versionFieldOptional = extractVersionField(entity);

        if (versionFieldOptional.isEmpty()) {
            return new VersionHandler(
                    entity,
                    buildUpdateQuery(null, document),
                    null,
                    false,
                    false,
                    null);
        }

        Field versionField = versionFieldOptional.get();
        Long versionValue = extractVersionValue(versionField, entity);
        String versionFieldName = versionField.getName();
        BsonDocument query = buildUpdateQuery(versionFieldName, document);

        return new VersionHandler(entity, query, versionField, true, versionValue != null, versionFieldName);
    }

    public boolean containsVersionAnnotationAndValue() {
        return containsVersionAnnotation && containsVersionValue;
    }

    private static Optional<Field> extractVersionField(Object entity) {
        Field[] fields = entity.getClass().getFields();
        Field[] declaredFields = entity.getClass().getDeclaredFields();
        return Stream.of(fields, declaredFields)
                .flatMap(Stream::of)
                .filter(field -> field.isAnnotationPresent(Version.class))
                .findFirst();
    }

    private static Long extractVersionValue(Field versionField, Object entity) {
        try {
            if (versionField == null) {
                return null;
            }

            versionField.setAccessible(true);
            Long versionValue = (Long) versionField.get(entity);
            return versionValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on set the version value");
        }
    }

    private VersionHandler adjustVersionValue() {
        try {
            if (!containsVersionAnnotation) {
                return this;
            }

            Long versionValue = (Long) versionField.get(entity);
            versionField.set(entity, versionValue == null ? 0L : versionValue + 1);
            return this;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on set the version value");
        }
    }

    /**
     * Utility method to check if the during an update there are documents not updated.
     */
    public boolean hasNotAffectedResults(Long quantityResultAffected) {
        return containsVersionAnnotation && quantityResultAffected == 0;
    }

    /**
     * We build the query that will be used to update the document, can have id, or id/version.
     */
    private static BsonDocument buildUpdateQuery(String versionFieldName, BsonDocument document) {
        //then we get its id field and create a new Document with only this one that will be our replace query
        BsonValue id = document.get(MongoOperations.ID);

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
}
