package io.quarkus.rest.data.panache.deployment.utils;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.gizmo.Type;

public final class TypeUtils {

    private static final Map<String, Class> PRIMITIVE_TO_CLASS_MAPPING = new HashMap<>();

    static {
        PRIMITIVE_TO_CLASS_MAPPING.put("int", Integer.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("byte", Byte.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("char", Character.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("short", Short.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("long", Long.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("float", Float.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("double", Double.class);
        PRIMITIVE_TO_CLASS_MAPPING.put("boolean", Boolean.class);
    }

    private TypeUtils() {

    }

    public static Object primitiveToClass(String type) {
        Class<?> clazz = PRIMITIVE_TO_CLASS_MAPPING.get(type);
        return clazz != null ? clazz : type;
    }

    public static Type toGizmoType(Object object) {
        if (object instanceof Type) {
            return (Type) object;
        } else if (object instanceof String) {
            return Type.classType((String) object);
        } else if (object instanceof Class) {
            return Type.classType((Class<?>) object);
        }

        throw new IllegalArgumentException(
                "Unsupported object of type " + object.getClass() + ". Supported types are Type, String and Class");
    }

}
