package io.quarkus.panache.common.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetamodelInfo {
    final Map<String, EntityModel> entities = new HashMap<>();

    public EntityModel getEntityModel(String className) {
        return entities.get(className);
    }

    public void addEntityModel(EntityModel entityModel) {
        entities.put(entityModel.name, entityModel);
    }

    public Set<String> getEntitiesWithPublicFields() {
        return entities.entrySet().stream()
                .filter(e -> !e.getValue().fields.isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
