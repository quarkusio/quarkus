package io.quarkus.reproducer.jacksonbuilder.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Model class with inheritance and builder.
 */
abstract class InheritedModelWithBuilderBase {

    // -------------------------------------------------------------------------
    // Class attributes
    // -------------------------------------------------------------------------

    private static ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Object attributes
    // -------------------------------------------------------------------------

    private final int version;
    private final String id;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected InheritedModelWithBuilderBase(final Builder builder) {
        this.version = (Integer) builder.version.orElse(1);
        this.id = builder.id;
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

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    abstract static class Builder<B extends Builder, T extends InheritedModelWithBuilderBase> {

        // -------------------------------------------------------------------------
        // Object attributes
        // -------------------------------------------------------------------------

        protected Optional<Integer> version = Optional.empty();
        protected String id;

        // -------------------------------------------------------------------------
        // Constructor
        // -------------------------------------------------------------------------

        protected Builder(@JsonProperty(value = "id", required = true) final String id) {
            this.id = id;
        }

        protected Builder(final T object) {
            this.version = Optional.of(object.getVersion());
            this.id = object.getId();
        }

        // -------------------------------------------------------------------------
        // Builder methods
        // -------------------------------------------------------------------------

        @JsonProperty(value = "version")
        @SuppressWarnings("unchecked")
        public B withVersion(int version) {
            this.version = Optional.of(version);
            return (B) this;
        }

        abstract public T build();
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    protected static ObjectMapper getObjectMapper() {
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
