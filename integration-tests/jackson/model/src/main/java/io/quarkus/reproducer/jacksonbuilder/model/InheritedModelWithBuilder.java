package io.quarkus.reproducer.jacksonbuilder.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Simple model class.
 */
@JsonPropertyOrder({ "version", "id", "value" })
@JsonDeserialize(builder = InheritedModelWithBuilder.Builder.class)
public class InheritedModelWithBuilder extends InheritedModelWithBuilderBase {

    // -------------------------------------------------------------------------
    // Object attributes
    // -------------------------------------------------------------------------

    private final String value;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    private InheritedModelWithBuilder(final Builder builder) {
        super(builder);
        this.value = builder.value;
    }

    // -------------------------------------------------------------------------
    // Interface
    // -------------------------------------------------------------------------

    public String toJson() throws IOException {
        String json = getObjectMapper().writeValueAsString(this);
        return json;
    }

    public static String toJson(final InheritedModelWithBuilder model) throws IOException {
        return model.toJson();
    }

    public static InheritedModelWithBuilder fromJson(final String json) throws IOException {
        return getObjectMapper().readerFor(InheritedModelWithBuilder.class).readValue(json);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @JsonProperty(value = "value")
    public String getValue() {
        return value;
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    public static final class Builder
            extends InheritedModelWithBuilderBase.Builder<Builder, InheritedModelWithBuilder> {

        // -------------------------------------------------------------------------
        // Object attributes
        // -------------------------------------------------------------------------

        protected String value = "";

        // -------------------------------------------------------------------------
        // Constructor
        // -------------------------------------------------------------------------

        public Builder(@JsonProperty(value = "id", required = true) final String id) {
            super(id);
        }

        public Builder(final InheritedModelWithBuilder object) {
            super(object);
            this.value = object.value;
        }

        // -------------------------------------------------------------------------
        // Builder methods
        // -------------------------------------------------------------------------

        @JsonProperty(value = "value", required = true)
        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public InheritedModelWithBuilder build() {
            return new InheritedModelWithBuilder(this);
        }
    }
}
