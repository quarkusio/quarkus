package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.catalog.Category;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonCategory implements Category.Mutable {

    protected String id;
    protected String name;
    protected String description;

    protected Map<String, Object> metadata;

    @Override
    public String getId() {
        return id;
    }

    public Mutable setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public Mutable setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Mutable setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? metadata = new HashMap<>() : metadata;
    }

    public Mutable setMetadata(Map<String, Object> newValues) {
        if (newValues != Collections.EMPTY_MAP) { // don't keep the empty map
            metadata = newValues;
        }
        return this;
    }

    public Mutable setMetadata(String key, Object value) {
        getMetadata().put(key, value);
        return this;
    }

    public Mutable removeMetadata(String key) {
        getMetadata().remove(key);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JsonCategory category = (JsonCategory) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public Mutable mutable() {
        return this;
    }

    @Override
    public JsonCategory build() {
        return this;
    }
}
