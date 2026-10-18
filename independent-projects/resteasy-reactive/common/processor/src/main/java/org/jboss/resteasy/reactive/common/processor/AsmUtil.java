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

    /**
     * Returns the bytecode instruction to load the given Jandex Type. This returns the specialised
     * bytecodes <tt>ILOAD, DLOAD, FLOAD and LLOAD</tt> for primitives, or <tt>ALOAD</tt> otherwise.
     *
     * @param jandexType The Jandex Type whose load instruction to return.
     * @return The bytecode instruction to load the given Jandex Type.
     */
    public static int getLoadOpcode(Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                    return Opcodes.ILOAD;
                case DOUBLE:
                    return Opcodes.DLOAD;
                case FLOAT:
                    return Opcodes.FLOAD;
                case LONG:
                    return Opcodes.LLOAD;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        }
        return Opcodes.ALOAD;
    }

    /**
     * Returns the bytecode instruction to store the given Jandex Type. This returns the specialised
     * bytecodes <tt>ISTORE, DSTORE, FSTORE and LSTORE</tt> for primitives, or <tt>ASTORE</tt> otherwise.
     *
     * @param jandexType The Jandex Type whose store instruction to return.
     * @return The bytecode instruction to store the given Jandex Type.
     */
    public static int getStoreOpcode(Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                    return Opcodes.ISTORE;
                case DOUBLE:
                    return Opcodes.DSTORE;
                case FLOAT:
                    return Opcodes.FSTORE;
                case LONG:
                    return Opcodes.LSTORE;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        }
        return Opcodes.ASTORE;
    }

    /**
     * Returns a null value suitable for initialising variables to null or its equivalent for primitives
     */
    public static int getNullValueOpcode(Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                    return Opcodes.ICONST_0;
                case DOUBLE:
                    return Opcodes.DCONST_0;
                case FLOAT:
                    return Opcodes.FCONST_0;
                case LONG:
                    return Opcodes.LCONST_0;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        }
        return Opcodes.ACONST_NULL;
    }

    /**
     * Returns the number of underlying bytecode parameters taken by the given Jandex parameter Type.
     * This will be 2 for doubles and longs, 1 otherwise.
     *
     * @param paramType the Jandex parameter Type
     * @return the number of underlying bytecode parameters required.
     */
    public static int getParameterSize(Type paramType) {
        if (paramType.kind() == Kind.PRIMITIVE) {
            switch (paramType.asPrimitiveType().primitive()) {
                case DOUBLE:
                case LONG:
                    return 2;
            }
        }
        return 1;
    }
}
