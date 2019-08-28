package io.quarkus.reproducer.jacksonbuilder.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Simple model class.
 */
@JsonPropertyOrder({ "version", "id", "value" })
@JsonDeserialize(builder = ModelWithBuilder.Builder.class)
public class ModelWithBuilder {

    // -------------------------------------------------------------------------
    // Class attributes
    // -------------------------------------------------------------------------

    private static ObjectMapper objectMapper;

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
        String json = getObjectMapper().writeValueAsString(this);
        return json;
    }

    public static String toJson(final ModelWithBuilder model) throws IOException {
        return model.toJson();
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
        if (null == objectMapper) {
            objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                    .registerModule(new ParameterNamesModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());
        }
        return objectMapper;
    }

}
