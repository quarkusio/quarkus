package io.quarkus.registry.catalog;

import java.util.Map;

public interface Category {

    String getId();

    String getName();

    String getDescription();

    Map<String, Object> getMetadata();
}
