package io.quarkus.it.jackson.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import io.quarkus.arc.Arc;

/**
 * Simple model class.
 */
@JsonPropertyOrder({ "version", "id", "value" })
@JsonDeserialize(builder = ModelWithBuilder.Builder.class)
public class ModelWithBuilder {

    // -------------------------------------------------------------------------
    // Object attributes
    // -------------------------------------------------------------------------

    private final int version;
    private final String id;
    private final String value;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    private ModelWithBuilder(final Builder builder) {
        this.version = builder.version;
        this.id = builder.id;
        this.value = builder.value;
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

    public static ModelWithBuilder fromJson(final String json) throws IOException {
        return getObjectMapper().readerFor(ModelWithBuilder.class).readValue(json);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @JsonProperty(value = "version")
    public Integer getVersion() {
        return version;
    }

    @JsonProperty(value = "id")
    public String getId() {
        return id;
    }

    @JsonProperty(value = "value")
    public String getValue() {
        return value;
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    public static final class Builder {

        // -------------------------------------------------------------------------
        // Object attributes
        // -------------------------------------------------------------------------

        private int version = 1;
        private String id;
        private String value = "";

        // -------------------------------------------------------------------------
        // Constructor
        // -------------------------------------------------------------------------

        public Builder(@JsonProperty(value = "id", required = true) final String id) {
            this.id = id;
        }

        public Builder(final ModelWithBuilder object) {
            this.version = object.version;
            this.id = object.id;
            this.value = object.value;
        }

        // -------------------------------------------------------------------------
        // Builder methods
        // -------------------------------------------------------------------------

        @JsonProperty(value = "version")
        public Builder withVersion(final int version) {
            this.version = version;
            return this;
        }

        @JsonProperty(value = "value", required = true)
        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public ModelWithBuilder build() {
            return new ModelWithBuilder(this);
        }
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private static ObjectMapper getObjectMapper() {
        return Arc.container().instance(ObjectMapper.class).get();
    }

}
