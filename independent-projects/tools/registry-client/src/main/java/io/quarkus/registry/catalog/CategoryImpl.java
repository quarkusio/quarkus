package io.quarkus.registry.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see Category#mutable() creates a builder from an existing Category
 * @see Category#builder() creates a builder
 * @see ExtensionCatalogImpl.Builder#getCategories() will use the builder to deserialize
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "id", "name", "description", "metadata" })
public class CategoryImpl implements Category {

    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Object> metadata;

    private CategoryImpl(Builder builder) {
        this.id = builder.id;
        if (id == null) {
            throw new IllegalArgumentException("id is missing for category named " + builder.name);
        }
        this.name = builder.name;
        this.description = builder.description;
        this.metadata = JsonBuilder.toUnmodifiableMap(builder.metadata);
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

    @Override
    public boolean equals(Object o) {
        return categoryEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, getMetadata());
    }

    @Override
    public String toString() {
        return categoryToString(this);
    }

    /**
     * Builder.
     */
    public static class Builder implements Category.Mutable {
        protected String id;
        protected String name;
        protected String description;
        protected Map<String, Object> metadata;

        Builder() {
        }

        @JsonIgnore
        Builder(Category config) {
            this.id = config.getId();
            this.name = config.getName();
            this.description = config.getDescription();
            this.metadata = config.getMetadata();
        }

        @Override
        public String getId() {
            return id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? metadata = new HashMap<>() : metadata;
        }

        @Override
        public Builder setMetadata(Map<String, Object> newValues) {
            metadata = JsonBuilder.modifiableMapOrNull(newValues, HashMap::new);
            return this;
        }

        @Override
        public Builder setMetadata(String name, Object value) {
            getMetadata().put(name, value);
            return this;
        }

        @Override
        public Builder removeMetadata(String key) {
            getMetadata().remove(key);
            return this;
        }

        @Override
        public CategoryImpl build() {
            return new CategoryImpl(this);
        }

        @Override
        public boolean equals(Object o) {
            return categoryEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, description, getMetadata());
        }

        @Override
        public String toString() {
            return categoryToString(this);
        }
    }

    static final boolean categoryEquals(Category c, Object o) {
        if (c == o)
            return true;
        if (!(o instanceof Category))
            return false;
        Category category = (Category) o;
        return Objects.equals(c.getId(), category.getId())
                && Objects.equals(c.getName(), category.getName())
                && Objects.equals(c.getDescription(), category.getDescription())
                && Objects.equals(c.getMetadata(), category.getMetadata());
    }

    static final String categoryToString(Category c) {
        return "Category{" +
                "id='" + c.getId() + '\'' +
                ", name='" + c.getName() + '\'' +
                ", description='" + c.getDescription() + '\'' +
                ", metadata=" + c.getMetadata() +
                ", builder=" + (c instanceof Builder) +
                '}';
    }
}
