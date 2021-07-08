package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.catalog.Category;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonCategory implements Category {

    protected String id;
    protected String name;
    protected String description;

    protected Map<String, Object> metadata;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? metadata = new HashMap<>() : metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
}
