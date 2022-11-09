package io.quarkus.rest.data.panache.deployment.utils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.rest.data.panache.deployment.ResourceMethodListenerBuildItem;

public final class ResourceMethodListenerUtils {

    private ResourceMethodListenerUtils() {

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
