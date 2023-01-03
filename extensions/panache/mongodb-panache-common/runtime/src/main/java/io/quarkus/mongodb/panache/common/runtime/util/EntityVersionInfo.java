package io.quarkus.mongodb.panache.common.runtime.util;

import java.lang.reflect.Field;

import org.bson.BsonDocument;
import org.bson.BsonValue;

/**
 * Utility class that contains the entity and useful info to
 * {@link VersionHandler @VersionHandler}
 * handle version when aplicable.
 * When an instance is created, by default the version value is already incremented.
 * VersionHandler is responsible to restore previous version value when some fail happens.
 **/
public class EntityVersionInfo {

    public final Object entity;
    public final BsonValue entityId;
    public final Field versionField;
    public final Long versionValue;
    public final BsonDocument updateQuery;
    public final boolean hasVersionAnnotation;
    public final boolean hasVersionValue;
    public final String versionFieldName;

    private EntityVersionInfo(final Object entity,
            BsonValue entityId,
            final BsonDocument updateQuery,
            Field versionField,
            Long versionValue,
            String versionFieldName) {
        this.entity = entity;
        this.entityId = entityId;
        this.updateQuery = updateQuery;
        this.versionField = versionField;
        this.versionValue = versionValue;
        this.hasVersionAnnotation = versionField != null;
        this.hasVersionValue = versionValue != null;
        this.versionFieldName = versionFieldName;
    }

    public static EntityVersionInfo of(final Object entity,
            BsonValue entityId,
            final BsonDocument updateQuery,
            Field versionField,
            Long versionValue,
            String versionFieldName) {
        return new EntityVersionInfo(
                entity,
                entityId,
                updateQuery,
                versionField,
                versionValue,
                versionFieldName);
    }

    /**
     * Constructor that ignores version annotation/value.
     */
    public static EntityVersionInfo of(Object entity, BsonValue entityId, BsonDocument updateQuery) {
        return new EntityVersionInfo(entity, entityId, updateQuery, null, null, null);
    }

    public boolean hasVersionAnnotationAndValue() {
        return hasVersionAnnotation && hasVersionValue;
    }
}
