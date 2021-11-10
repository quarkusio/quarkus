package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = CategoryImpl.Builder.class)
public class CategoryImpl extends CatalogMetadata implements Category {

    private final String id;
    private final String name;
    private final String description;

    private CategoryImpl(Builder builder) {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements Category {
        protected String id;
        protected String name;
        protected String description;

        public Builder() {
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public CategoryImpl build() {
            return new CategoryImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CategoryImpl category = (CategoryImpl) o;
        return Objects.equals(id, category.id)
                && Objects.equals(name, category.name)
                && Objects.equals(description, category.description)
                && Objects.equals(metadata, category.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, metadata);
    }

    @Override
    public String toString() {
        return "CategoryImpl{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
