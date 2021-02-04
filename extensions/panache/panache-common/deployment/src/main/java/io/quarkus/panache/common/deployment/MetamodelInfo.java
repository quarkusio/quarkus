package io.quarkus.panache.common.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetamodelInfo {
    final Map<String, EntityModel> entities = new HashMap<>();

    public EntityModel getEntityModel(String className) {
        return entities.get(className);
    }

    public void addEntityModel(EntityModel entityModel) {
        entities.put(entityModel.name, entityModel);
    }

    public boolean hasEntities() {
        return !entities.isEmpty();
    }

    public Set<String> getEntityClassNames() {
        return entities.keySet();
    }
}
