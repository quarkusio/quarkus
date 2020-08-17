package io.quarkus.rest.data.panache.deployment;

import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.FieldInfo;

import io.quarkus.gizmo.MethodDescriptor;

public final class RestDataEntityInfo {

    private final String type;

    private final String idType;

    private final FieldInfo idField;

    private final MethodDescriptor idSetter;

    public RestDataEntityInfo(String type, String idType, FieldInfo idField, MethodDescriptor idSetter) {
        Objects.requireNonNull(type, "Entity type is required");
        Objects.requireNonNull(idType, "Id type is required");
        Objects.requireNonNull(idField, "Id field is required");
        this.type = type;
        this.idType = idType;
        this.idField = idField;
        this.idSetter = idSetter;
    }

    public String getType() {
        return type;
    }

    public String getIdType() {
        return idType;
    }

    public FieldInfo getIdField() {
        return idField;
    }

    public Optional<MethodDescriptor> getIdSetter() {
        return Optional.ofNullable(idSetter);
    }
}
