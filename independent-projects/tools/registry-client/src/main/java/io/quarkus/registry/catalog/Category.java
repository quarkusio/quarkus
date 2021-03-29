package io.quarkus.registry.catalog;

import java.util.Map;

public interface Category {

    String MD_PINNED = "pinned";

    String getId();

    String getName();

    String getDescription();

    Map<String, Object> getMetadata();
}
