package io.quarkus.resteasy.reactive.links.deployment;

import java.util.Objects;

import org.jboss.jandex.FieldInfo;

class GetterMetadata {

    private static final String GETTER_PREFIX = "resteasy_links_get_";

    private static final String ACCESSOR_SUFFIX = "$_resteasy_links";

    private final String entityType;

    private final String fieldType;

    private final String fieldName;

    GetterMetadata(FieldInfo fieldInfo) {
        this.entityType = fieldInfo.declaringClass().toString();
        this.fieldType = fieldInfo.type().name().toString();
        this.fieldName = fieldInfo.name();
    }

    public String getEntityType() {
        return entityType;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getGetterName() {
        return GETTER_PREFIX + fieldName;
    }

    public String getGetterAccessorName() {
        return entityType + ACCESSOR_SUFFIX + getGetterName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GetterMetadata that = (GetterMetadata) o;
        return entityType.equals(that.entityType)
                && fieldType.equals(that.fieldType)
                && fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityType, fieldType, fieldName);
    }
}
