package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

final class JacksonSerializationUtils {

    private static boolean DEFAULT_BOOLEAN;
    private static byte DEFAULT_BYTE;
    private static char DEFAULT_CHAR;
    private static double DEFAULT_DOUBLE;
    private static float DEFAULT_FLOAT;
    private static int DEFAULT_INT;
    private static long DEFAULT_LONG;
    private static short DEFAULT_SHORT;

    private JacksonSerializationUtils() {
    }

    static boolean isBoxedPrimitive(Type type) {
        return isBoxedPrimitive(type.name().toString());
    }

    static boolean isBoxedPrimitive(String typeName) {
        return "java.lang.Character".equals(typeName) || "java.lang.Short".equals(typeName)
                || "java.lang.Integer".equals(typeName) || "java.lang.Long".equals(typeName)
                || "java.lang.Float".equals(typeName) || "java.lang.Double".equals(typeName)
                || "java.lang.Boolean".equals(typeName);
    }

    static boolean isBasicJsonType(Type type) {
        if (type.kind() == Kind.PRIMITIVE) {
            return true;
        }
        if (isBoxedPrimitive(type)) {
            return true;
        }
        if ("java.lang.String".equals(type.name().toString())) {
            return true;
        }

        return false;
    }

    static ResultHandle getDefaultValue(BytecodeCreator bytecodeCreator, Type type) {
        if (type.kind() != Kind.PRIMITIVE) {
            return bytecodeCreator.loadNull();
        }

        return switch (type.name().toString()) {
            case "byte" -> bytecodeCreator.load(DEFAULT_BYTE);
            case "boolean" -> bytecodeCreator.load(DEFAULT_BOOLEAN);
            case "char" -> bytecodeCreator.load(DEFAULT_CHAR);
            case "double" -> bytecodeCreator.load(DEFAULT_DOUBLE);
            case "float" -> bytecodeCreator.load(DEFAULT_FLOAT);
            case "int" -> bytecodeCreator.load(DEFAULT_INT);
            case "long" -> bytecodeCreator.load(DEFAULT_LONG);
            case "short" -> bytecodeCreator.load(DEFAULT_SHORT);
            default -> throw new IllegalStateException("Type " + type + " should be handled by the switch");
        };
    }
}
