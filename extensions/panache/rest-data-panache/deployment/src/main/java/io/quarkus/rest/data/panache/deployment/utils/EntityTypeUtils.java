package io.quarkus.rest.data.panache.deployment.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.rest.data.panache.deployment.ResourceMethodListenerBuildItem;

public final class EntityTypeUtils {

    private EntityTypeUtils() {

    }

    public static Map<String, Type> getEntityFields(IndexView index, String entityTypeName) {
        Map<String, Type> fields = new HashMap<>();
        ClassInfo currentEntityClass = index.getClassByName(entityTypeName);
        while (currentEntityClass != null) {
            for (FieldInfo field : currentEntityClass.fields()) {
                fields.put(field.name(), field.type());
            }

            if (currentEntityClass.superName() != null) {
                currentEntityClass = index.getClassByName(currentEntityClass.superName());
            } else {
                currentEntityClass = null;
            }
        }

        return fields;
    }

    public static List<ClassInfo> getListenersByEntityType(IndexView index,
            List<ResourceMethodListenerBuildItem> resourceMethodListeners,
            String entityTypeName) {
        ClassInfo entityClass = index.getClassByName(entityTypeName);
        return resourceMethodListeners.stream()
                .filter(isCompatibleWithEntityType(index, entityClass))
                .map(e -> e.getClassInfo())
                .collect(Collectors.toList());
    }

    private static Predicate<ResourceMethodListenerBuildItem> isCompatibleWithEntityType(IndexView index,
            ClassInfo entityClass) {
        return e -> {
            DotName entityTypeOfListener = e.getEntityType().asClassType().name();
            ClassInfo currentEntityClass = entityClass;
            while (currentEntityClass != null) {
                if (entityTypeOfListener.equals(currentEntityClass.name())) {
                    return true;
                }

                if (currentEntityClass.superName() != null) {
                    currentEntityClass = index.getClassByName(currentEntityClass.superName());
                } else {
                    currentEntityClass = null;
                }
            }

            return false;
        };
    }
}
