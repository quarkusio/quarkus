package io.quarkus.registry.catalog;

import java.util.Map;

import io.quarkus.registry.json.JsonBuilder;

public interface Category {

    String MD_PINNED = "pinned";

    String getId();

    String getName();

    String getDescription();

    Map<String, Object> getMetadata();

    default Mutable mutable() {
        return new CategoryImpl.Builder(this);
    }

    interface Mutable extends Category, JsonBuilder<Category> {

        Mutable setId(String id);

        Mutable setName(String name);

        Mutable setDescription(String description);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String name, Object value);

        Mutable removeMetadata(String key);

        @Override
        Category build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new CategoryImpl.Builder();
    }
}
