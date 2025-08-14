package io.quarkus.panache.common.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.panache.common.deployment.EntityField.Visibility;

public class MetamodelInfo {
    final Map<String, EntityModel> entities = new HashMap<>();

    public EntityModel getEntityModel(String className) {
        return entities.get(className);
    }

    public void addEntityModel(EntityModel entityModel) {
        entities.put(entityModel.name, entityModel);
    }

    public Set<String> getEntitiesWithExternallyAccessibleFields() {
        return entities.entrySet().stream()
                .filter(e -> {
                    EntityModel value = e.getValue();
                    for (;;) {
                        // to be extra safe, we just avoid to consider private fields
                        // so we also support package-private fields
                        if (value.fields.values().stream().anyMatch(f -> f.visibility != Visibility.PRIVATE)) {
                            return true;
                        }
                        if (value.superClassName == null) {
                            return false;
                        }
                        value = entities.get(value.superClassName);
                        if (value == null) {
                            return false;
                        }
                    }
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
