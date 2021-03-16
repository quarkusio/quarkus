package io.quarkus.it.jackson.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Simple POJO model class.
 */
@RegisterForReflection
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class RegisteredPojoModel {

    // -------------------------------------------------------------------------
    // Object attributes
    // -------------------------------------------------------------------------

    private int version = 1;
    private String id = null;
    private String value = null;

    private RegisteredPojoModel parent;
    private RegisteredPojoModel child;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public RegisteredPojoModel() {
    }

    // -------------------------------------------------------------------------
    // Interface
    // -------------------------------------------------------------------------

    public String toJson() throws IOException {
        return toJson(getObjectMapper());
    }

    public String toJson(ObjectMapper objectMapper) throws IOException {
        return objectMapper.writeValueAsString(this);
    }

    public static RegisteredPojoModel fromJson(final String json) throws IOException {
        return getObjectMapper().readerFor(RegisteredPojoModel.class).readValue(json);
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RegisteredPojoModel getParent() {
        return parent;
    }

    public void setParent(RegisteredPojoModel parent) {
        this.parent = parent;
    }

    public RegisteredPojoModel getChild() {
        return child;
    }

    public void setChild(RegisteredPojoModel child) {
        this.child = child;
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private static ObjectMapper getObjectMapper() {
        return Arc.container().instance(ObjectMapper.class).get();
    }

}
