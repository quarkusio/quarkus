package io.quarkus.it.jackson.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class with inheritance and builder.
 */
abstract class InheritedModelWithBuilderBase {

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

}
