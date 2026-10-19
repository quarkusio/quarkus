package io.quarkus.rest.data.panache.deployment.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
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

    // Serialization annotations for filtering non-exposed fields
    private static final DotName JSON_IGNORE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    private static final DotName JSONB_TRANSIENT = DotName.createSimple("jakarta.json.bind.annotation.JsonbTransient");
    private static final DotName XML_TRANSIENT = DotName.createSimple("jakarta.xml.bind.annotation.XmlTransient");
    private static final DotName JSON_IGNORE_PROPERTIES = DotName
            .createSimple("com.fasterxml.jackson.annotation.JsonIgnoreProperties");
    private static final DotName JSON_IGNORE_TYPE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnoreType");
    private static final DotName SECURE_FIELD = DotName
            .createSimple("io.quarkus.resteasy.reactive.jackson.SecureField");

    private EntityTypeUtils() {

    }

    public static Map<String, Type> getEntityFields(IndexView index, String entityTypeName) {
        Map<String, Type> fields = new HashMap<>();
        ClassInfo currentEntityClass = index.getClassByName(entityTypeName);
        Set<String> ignoredFieldNames = getJsonIgnorePropertiesFields(index, currentEntityClass);

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
                // skip fields not exposed for serialization
                if (!isExposedForSerialization(index, field, ignoredFieldNames)) {
                    continue;
                }

                // Only add direct fields - nested properties (e.g., author.id) are not supported
                // for sorting and filtering via query parameters
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

    /**
     * Checks if a field is exposed for serialization (not hidden by Jackson, JSON-B, or JAXB annotations).
     */
    private static boolean isExposedForSerialization(IndexView index, FieldInfo field, Set<String> ignoredFieldNames) {
        // Check field-level annotations
        if (field.hasAnnotation(JSON_IGNORE)
                || field.hasAnnotation(JSONB_TRANSIENT)
                || field.hasAnnotation(XML_TRANSIENT)) {
            return false;
        }

        // Check if field name is listed in @JsonIgnoreProperties on the class
        if (ignoredFieldNames.contains(field.name())) {
            return false;
        }

        // Check if the field's type is annotated with @JsonIgnoreType
        if (field.type().kind() == Type.Kind.CLASS) {
            ClassInfo fieldTypeClass = index.getClassByName(field.type().name());
            if (fieldTypeClass != null && fieldTypeClass.hasAnnotation(JSON_IGNORE_TYPE)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Collects field names listed in @JsonIgnoreProperties on the entity class and its superclasses.
     */
    private static Set<String> getJsonIgnorePropertiesFields(IndexView index, ClassInfo entityClass) {
        Set<String> ignoredFields = new HashSet<>();
        ClassInfo currentClass = entityClass;

        while (currentClass != null) {
            AnnotationInstance annotation = currentClass.annotation(JSON_IGNORE_PROPERTIES);
            if (annotation != null) {
                AnnotationValue value = annotation.value();
                if (value != null) {
                    String[] fieldNames = value.asStringArray();
                    ignoredFields.addAll(Arrays.asList(fieldNames));
                }
            }

            if (currentClass.superName() != null) {
                currentClass = index.getClassByName(currentClass.superName());
            } else {
                currentClass = null;
            }
        }

        return ignoredFields;
    }

    /**
     * Returns a map of field names to their required roles from @SecureField annotations.
     * Format is suitable for the SortQueryParamValidator annotation: "fieldName:role1,role2"
     */
    public static Map<String, String> getSecureFields(IndexView index, String entityTypeName) {
        Map<String, String> secureFields = new HashMap<>();
        ClassInfo currentEntityClass = index.getClassByName(entityTypeName);

        while (currentEntityClass != null) {
            for (FieldInfo field : currentEntityClass.fields()) {
                // skip static fields
                if ((field.flags() & ACC_STATIC) != 0) {
                    continue;
                }

                AnnotationInstance secureFieldAnnotation = field.annotation(SECURE_FIELD);
                if (secureFieldAnnotation != null) {
                    AnnotationValue rolesValue = secureFieldAnnotation.value("rolesAllowed");
                    if (rolesValue != null) {
                        String[] roles = rolesValue.asStringArray();
                        if (roles.length > 0) {
                            secureFields.put(field.name(), String.join(",", roles));
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

        return secureFields;
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
