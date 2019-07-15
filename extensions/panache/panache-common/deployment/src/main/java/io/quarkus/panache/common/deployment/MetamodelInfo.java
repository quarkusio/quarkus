package io.quarkus.panache.common.deployment;

import java.util.HashMap;
import java.util.Map;

public class MetamodelInfo<EntityModelType extends EntityModel<?>> {
    final Map<String, EntityModelType> entities = new HashMap<>();

    public EntityModelType getEntityModel(String className) {
        return entities.get(className);
    }

    public void addEntityModel(EntityModelType entityModel) {
        entities.put(entityModel.name, entityModel);
    }

    public boolean hasEntities() {
        return !entities.isEmpty();
    }
}
