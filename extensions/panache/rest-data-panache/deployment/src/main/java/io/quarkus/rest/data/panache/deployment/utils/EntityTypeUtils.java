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

    // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.5
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_FINAL = 0x0010;

    private EntityTypeUtils() {

    }

    public static Map<String, Type> getEntityFields(IndexView index, String entityTypeName) {
        Map<String, Type> fields = new HashMap<>();
        ClassInfo currentEntityClass = index.getClassByName(entityTypeName);
        while (currentEntityClass != null) {
            for (FieldInfo field : currentEntityClass.fields()) {
                // skip static fields
                if ((field.flags() & ACC_STATIC) != 0) {
                    continue;
                }
                // skip final fields
                if ((field.flags() & ACC_FINAL) != 0) {
                    continue;
                }
                // skip fields with Transient annotation
                if (field.hasAnnotation(DotName.createSimple("jakarta.persistence.Transient"))) {
                    continue;
                }

                fields.put(field.name(), field.type());

                // if the field is a ManyToOne relation, add the Id field of the relation to the fields map
                if (field.type().kind() == Type.Kind.CLASS
                        && field.hasAnnotation(DotName.createSimple("jakarta.persistence.ManyToOne"))) {
                    // get the class info for the relation field
                    ClassInfo currentRelationClass = index.getClassByName(field.type().name());
                    while (currentRelationClass != null) {
                        // get the field with Id annotation
                        FieldInfo relationIdField = currentRelationClass.fields().stream().filter((relationField) -> {
                            return relationField.hasAnnotation(DotName.createSimple("jakarta.persistence.Id"));
                        }).findFirst().orElse(null);
                        // if the field is not null, add it to the fields map
                        if (relationIdField != null) {
                            fields.put(field.name() + "." + relationIdField.name(), relationIdField.type());
                        }

                        // get the super class of the relation class
                        if (currentRelationClass.superName() != null) {
                            currentRelationClass = index.getClassByName(currentRelationClass.superName());
                        } else {
                            currentRelationClass = null;
                        }
                    }

                }

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
