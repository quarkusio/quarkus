package org.jboss.resteasy.reactive.common.processor;

import java.util.function.Function;

import org.jboss.jandex.GenericSignature;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A collection of ASM and Jandex utilities.
 */
public class AsmUtil {
    public static String getSignature(Type type) {
        StringBuilder result = new StringBuilder();
        GenericSignature.forType(type, GenericSignature.NO_SUBSTITUTION, result);
        return result.toString();
    }

    public static String getSignature(Type type, Function<String, Type> typeVariableSubstitution) {
        StringBuilder result = new StringBuilder();
        GenericSignature.forType(type, typeVariableSubstitution, result);
        return result.toString();
    }

    /**
     * Calls the right unboxing method for the given Jandex Type if it is a primitive.
     *
     * @param mv The MethodVisitor on which to visit the unboxing instructions
     * @param jandexType The Jandex Type to unbox if it is a primitive.
     */
    public static void unboxIfRequired(MethodVisitor mv, Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                    unbox(mv, "java/lang/Boolean", "booleanValue", "Z");
                    break;
                case BYTE:
                    unbox(mv, "java/lang/Byte", "byteValue", "B");
                    break;
                case CHAR:
                    unbox(mv, "java/lang/Character", "charValue", "C");
                    break;
                case DOUBLE:
                    unbox(mv, "java/lang/Double", "doubleValue", "D");
                    break;
                case FLOAT:
                    unbox(mv, "java/lang/Float", "floatValue", "F");
                    break;
                case INT:
                    unbox(mv, "java/lang/Integer", "intValue", "I");
                    break;
                case LONG:
                    unbox(mv, "java/lang/Long", "longValue", "J");
                    break;
                case SHORT:
                    unbox(mv, "java/lang/Short", "shortValue", "S");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        }
    }

    /**
     * Calls the right unboxing method for the given Jandex Type if it is a primitive.
     *
     * @param mv The MethodVisitor on which to visit the unboxing instructions
     * @param type The Jandex Type to unbox if it is a primitive.
     */
    public static void unboxIfRequired(MethodVisitor mv, org.objectweb.asm.Type type) {
        if (type.getSort() <= org.objectweb.asm.Type.DOUBLE) {
            switch (type.getSort()) {
                case org.objectweb.asm.Type.BOOLEAN:
                    unbox(mv, "java/lang/Boolean", "booleanValue", "Z");
                    break;
                case org.objectweb.asm.Type.BYTE:
                    unbox(mv, "java/lang/Byte", "byteValue", "B");
                    break;
                case org.objectweb.asm.Type.CHAR:
                    unbox(mv, "java/lang/Character", "charValue", "C");
                    break;
                case org.objectweb.asm.Type.DOUBLE:
                    unbox(mv, "java/lang/Double", "doubleValue", "D");
                    break;
                case org.objectweb.asm.Type.FLOAT:
                    unbox(mv, "java/lang/Float", "floatValue", "F");
                    break;
                case org.objectweb.asm.Type.INT:
                    unbox(mv, "java/lang/Integer", "intValue", "I");
                    break;
                case org.objectweb.asm.Type.LONG:
                    unbox(mv, "java/lang/Long", "longValue", "J");
                    break;
                case org.objectweb.asm.Type.SHORT:
                    unbox(mv, "java/lang/Short", "shortValue", "S");
                    break;
            }
        }
    }

    private static void unbox(MethodVisitor mv, String owner, String methodName, String returnTypeSignature) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, owner);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, methodName, "()" + returnTypeSignature, false);
    }

}
