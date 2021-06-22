package io.quarkus.deployment.util;

import static java.util.Arrays.asList;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.WildcardType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A collection of ASM and Jandex utilities.
 * NOTE: this has a copy in AsmUtilCopy in arc-processor with some extra methods for knowing if we need a
 * signature and getting the signature of a class.
 */
public class AsmUtil {

    public static final List<org.objectweb.asm.Type> PRIMITIVES = asList(
            VOID_TYPE,
            BOOLEAN_TYPE,
            CHAR_TYPE,
            BYTE_TYPE,
            SHORT_TYPE,
            INT_TYPE,
            FLOAT_TYPE,
            LONG_TYPE,
            DOUBLE_TYPE);
    public static final List<org.objectweb.asm.Type> WRAPPERS = asList(
            getType(Void.class),
            getType(Boolean.class),
            getType(Character.class),
            getType(Byte.class),
            getType(Short.class),
            getType(Integer.class),
            getType(Float.class),
            getType(Long.class),
            getType(Double.class));
    public static final Map<org.objectweb.asm.Type, org.objectweb.asm.Type> WRAPPER_TO_PRIMITIVE = new HashMap<>();

    static {
        for (int i = 0; i < AsmUtil.PRIMITIVES.size(); i++) {
            AsmUtil.WRAPPER_TO_PRIMITIVE.put(AsmUtil.WRAPPERS.get(i), AsmUtil.PRIMITIVES.get(i));
        }
    }

    public static org.objectweb.asm.Type autobox(org.objectweb.asm.Type primitive) {
        return WRAPPERS.get(primitive.getSort());
    }

    /**
     * Returns the Java bytecode signature of a given Jandex MethodInfo.
     * If the Java compiler doesn't have to emit a signature for the method, {@code null} is returned instead.
     *
     * @param method the method you want the signature for
     * @return a bytecode signature for that method, or {@code null} if signature is not required
     */
    public static String getSignatureIfRequired(MethodInfo method) {
        return getSignatureIfRequired(method, ignored -> null);
    }

    /**
     * Returns the Java bytecode signature of a given Jandex MethodInfo using the given type argument mappings.
     * If the Java compiler doesn't have to emit a signature for the method, {@code null} is returned instead.
     *
     * @param method the method you want the signature for
     * @param typeArgMapper a mapping between type argument names and their bytecode signatures
     * @return a bytecode signature for that method, or {@code null} if signature is not required
     */
    public static String getSignatureIfRequired(MethodInfo method, Function<String, String> typeArgMapper) {
        if (!hasSignature(method)) {
            return null;
        }

        return getSignature(method, typeArgMapper);
    }

    private static boolean hasSignature(MethodInfo method) {
        // JVMS 16, chapter 4.7.9.1. Signatures:
        //
        // Java compiler must emit ...
        //
        // A method signature for any method or constructor declaration which is either generic,
        // or has a type variable or parameterized type as the return type or a formal parameter type,
        // or has a type variable in a throws clause, or any combination thereof.

        if (!method.typeParameters().isEmpty()) {
            return true;
        }

        {
            Type type = method.returnType();
            if (type.kind() == Kind.TYPE_VARIABLE
                    || type.kind() == Kind.UNRESOLVED_TYPE_VARIABLE
                    || type.kind() == Kind.PARAMETERIZED_TYPE) {
                return true;
            }
        }

        for (Type type : method.parameters()) {
            if (type.kind() == Kind.TYPE_VARIABLE
                    || type.kind() == Kind.UNRESOLVED_TYPE_VARIABLE
                    || type.kind() == Kind.PARAMETERIZED_TYPE) {
                return true;
            }
        }

        if (hasThrowsSignature(method)) {
            return true;
        }

        return false;
    }

    private static boolean hasThrowsSignature(MethodInfo method) {
        // JVMS 16, chapter 4.7.9.1. Signatures:
        //
        // If the throws clause of a method or constructor declaration does not involve type variables,
        // then a compiler may treat the declaration as having no throws clause for the purpose of
        // emitting a method signature.

        // also, no need to check if an exception type is of kind PARAMETERIZED_TYPE, because
        //
        // JLS 16, chapter 8.1.2. Generic Classes and Type Parameters:
        //
        // It is a compile-time error if a generic class is a direct or indirect subclass of Throwable.

        for (Type type : method.exceptions()) {
            if (type.kind() == Kind.TYPE_VARIABLE
                    || type.kind() == Kind.UNRESOLVED_TYPE_VARIABLE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the Java bytecode signature of a given Jandex MethodInfo using the given type argument mappings.
     * For example, given this method:
     *
     * <pre>
     * {@code
     * public class Foo<T> {
     *  public <R> List<R> method(int a, T t){...}
     * }
     * }
     * </pre>
     *
     * This will return <tt>&lt;R:Ljava/lang/Object;>(ILjava/lang/Integer;)Ljava/util/List&lt;TR;>;</tt> if
     * your {@code typeArgMapper} contains {@code T=Ljava/lang/Integer;}.
     *
     * @param method the method you want the signature for.
     * @param typeArgMapper a mapping between type argument names and their bytecode signature.
     * @return a bytecode signature for that method.
     */
    public static String getSignature(MethodInfo method, Function<String, String> typeArgMapper) {
        // for grammar, see JVMS 16, chapter 4.7.9.1. Signatures

        StringBuilder signature = new StringBuilder();

        if (!method.typeParameters().isEmpty()) {
            signature.append('<');
            for (TypeVariable typeParameter : method.typeParameters()) {
                typeParameter(typeParameter, signature, typeArgMapper);
            }
            signature.append('>');
        }

        signature.append('(');
        for (Type type : method.parameters()) {
            toSignature(signature, type, typeArgMapper, false);
        }
        signature.append(')');

        toSignature(signature, method.returnType(), typeArgMapper, false);

        if (hasThrowsSignature(method)) {
            for (Type exception : method.exceptions()) {
                signature.append('^');
                toSignature(signature, exception, typeArgMapper, false);
            }
        }

        return signature.toString();
    }

    private static void typeParameter(TypeVariable typeParameter, StringBuilder result,
            Function<String, String> typeArgMapper) {
        result.append(typeParameter.identifier());

        if (hasImplicitObjectBound(typeParameter)) {
            result.append(':');
        }
        for (Type bound : typeParameter.bounds()) {
            result.append(':');
            toSignature(result, bound, typeArgMapper, false);
        }
    }

    private static boolean hasImplicitObjectBound(TypeVariable typeParameter) {
        // TODO is there a better way? :-/
        boolean result = false;
        try {
            Method method = TypeVariable.class.getDeclaredMethod("hasImplicitObjectBound");
            method.setAccessible(true);
            result = (Boolean) method.invoke(typeParameter);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Returns the Java bytecode descriptor of a given Jandex MethodInfo using the given type argument mappings.
     * For example, given this method:
     * 
     * <pre>
     * {@code
     * public class Foo<T> {
     *  public <R> List<R> method(int a, T t){...} 
     * }
     * }
     * </pre>
     * 
     * This will return <tt>(ILjava/lang/Integer;)Ljava/util/List;</tt> if
     * your {@code typeArgMapper} contains {@code T=Ljava/lang/Integer;}.
     * 
     * @param method the method you want the descriptor for.
     * @param typeArgMapper a mapping between type argument names and their bytecode descriptor.
     * @return a bytecode descriptor for that method.
     */
    public static String getDescriptor(MethodInfo method, Function<String, String> typeArgMapper) {
        List<Type> parameters = method.parameters();

        StringBuilder descriptor = new StringBuilder("(");
        for (Type type : parameters) {
            toSignature(descriptor, type, typeArgMapper, true);
        }
        descriptor.append(")");
        toSignature(descriptor, method.returnType(), typeArgMapper, true);
        return descriptor.toString();
    }

    /**
     * Returns the Java bytecode descriptor of a given Jandex Type using the given type argument mappings.
     * For example, given this type: <tt>List&lt;T></tt>, this will return <tt>Ljava/util/List;</tt> if
     * your {@code typeArgMapper} contains {@code T=Ljava/lang/Integer;}.
     * 
     * @param type the type you want the descriptor for.
     * @param typeArgMapper a mapping between type argument names and their bytecode descriptor.
     * @return a bytecode descriptor for that type.
     */
    public static String getDescriptor(Type type, Function<String, String> typeArgMapper) {
        StringBuilder sb = new StringBuilder();
        toSignature(sb, type, typeArgMapper, true);
        return sb.toString();
    }

    /**
     * Returns the Java bytecode signature of a given Jandex Type using the given type argument mappings.
     * For example, given this type: <tt>List&lt;T></tt>, this will return <tt>Ljava/util/List&lt;Ljava/lang/Integer;>;</tt> if
     * your {@code typeArgMapper} contains {@code T=Ljava/lang/Integer;}.
     * 
     * @param type the type you want the signature for.
     * @param typeArgMapper a mapping between type argument names and their bytecode descriptor.
     * @return a bytecode signature for that type.
     */
    public static String getSignature(Type type, Function<String, String> typeArgMapper) {
        StringBuilder sb = new StringBuilder();
        toSignature(sb, type, typeArgMapper, false);
        return sb.toString();
    }

    private static void toSignature(StringBuilder sb, Type type, Function<String, String> typeArgMapper, boolean erased) {
        switch (type.kind()) {
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                for (int i = 0; i < arrayType.dimensions(); i++) {
                    sb.append('[');
                }
                toSignature(sb, arrayType.component(), typeArgMapper, erased);
                break;
            case CLASS:
                sb.append('L').append(type.asClassType().name().toString('/')).append(';');
                break;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                Type owner = parameterizedType.owner();
                if (owner != null && owner.kind() == Kind.PARAMETERIZED_TYPE) {
                    toSignature(sb, owner, typeArgMapper, erased);
                    // the typeSignature call on previous line always takes the PARAMETERIZED_TYPE branch,
                    // so at this point, result ends with a ';', which we just replace with '.'
                    assert sb.charAt(sb.length() - 1) == ';';
                    sb.setCharAt(sb.length() - 1, '.');
                    sb.append(parameterizedType.name().local());
                } else {
                    sb.append('L').append(parameterizedType.name().toString('/'));
                }
                if (!erased && !parameterizedType.arguments().isEmpty()) {
                    sb.append('<');
                    for (Type argument : parameterizedType.arguments()) {
                        toSignature(sb, argument, typeArgMapper, erased);
                    }
                    sb.append('>');
                }
                sb.append(';');
                break;
            case PRIMITIVE:
                Primitive primitive = type.asPrimitiveType().primitive();
                switch (primitive) {
                    case BOOLEAN:
                        sb.append('Z');
                        break;
                    case BYTE:
                        sb.append('B');
                        break;
                    case CHAR:
                        sb.append('C');
                        break;
                    case DOUBLE:
                        sb.append('D');
                        break;
                    case FLOAT:
                        sb.append('F');
                        break;
                    case INT:
                        sb.append('I');
                        break;
                    case LONG:
                        sb.append('J');
                        break;
                    case SHORT:
                        sb.append('S');
                        break;
                }
                break;
            case TYPE_VARIABLE:
                TypeVariable typeVariable = type.asTypeVariable();
                String mappedSignature = typeArgMapper.apply(typeVariable.identifier());
                if (mappedSignature != null) {
                    sb.append(mappedSignature);
                } else if (erased) {
                    toSignature(sb, typeVariable.bounds().get(0), typeArgMapper, erased);
                } else {
                    sb.append('T').append(typeVariable.identifier()).append(';');
                }
                break;
            case UNRESOLVED_TYPE_VARIABLE:
                UnresolvedTypeVariable unresolvedTypeVariable = type.asUnresolvedTypeVariable();
                String mappedSignature2 = typeArgMapper.apply(unresolvedTypeVariable.identifier());
                if (mappedSignature2 != null) {
                    sb.append(mappedSignature2);
                } else if (erased) {
                    // TODO ???
                } else {
                    sb.append("T").append(unresolvedTypeVariable.identifier()).append(";");
                }
                break;
            case VOID:
                sb.append('V');
                break;
            case WILDCARD_TYPE:
                if (!erased) {
                    WildcardType wildcardType = type.asWildcardType();
                    if (wildcardType.superBound() != null) {
                        sb.append('-');
                        toSignature(sb, wildcardType.superBound(), typeArgMapper, erased);
                    } else if (ClassType.OBJECT_TYPE.equals(wildcardType.extendsBound())) {
                        sb.append('*');
                    } else {
                        sb.append('+');
                        toSignature(sb, wildcardType.extendsBound(), typeArgMapper, erased);
                    }
                }
                break;
            default:
                break;

        }
    }

    /**
     * Returns a return bytecode instruction suitable for the given return type descriptor. This will return
     * specialised return instructions <tt>IRETURN, LRETURN, FRETURN, DRETURN, RETURN</tt> for primitives/void,
     * and <tt>ARETURN</tt> otherwise;
     * 
     * @param typeDescriptor the return type descriptor.
     * @return the correct bytecode return instruction for that return type descriptor.
     */
    public static int getReturnInstruction(String typeDescriptor) {
        switch (typeDescriptor) {
            case "Z":
            case "B":
            case "C":
            case "S":
            case "I":
                return Opcodes.IRETURN;
            case "J":
                return Opcodes.LRETURN;
            case "F":
                return Opcodes.FRETURN;
            case "D":
                return Opcodes.DRETURN;
            case "V":
                return Opcodes.RETURN;
            default:
                return Opcodes.ARETURN;
        }
    }

    /**
     * Returns a return bytecode instruction suitable for the given return Jandex Type. This will return
     * specialised return instructions <tt>IRETURN, LRETURN, FRETURN, DRETURN, RETURN</tt> for primitives/void,
     * and <tt>ARETURN</tt> otherwise;
     * 
     * @param jandexType the return Jandex Type.
     * @return the correct bytecode return instruction for that return type descriptor.
     */
    public static int getReturnInstruction(Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                    return Opcodes.IRETURN;
                case DOUBLE:
                    return Opcodes.DRETURN;
                case FLOAT:
                    return Opcodes.FRETURN;
                case LONG:
                    return Opcodes.LRETURN;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        } else if (jandexType.kind() == Kind.VOID) {
            return Opcodes.RETURN;
        }
        return Opcodes.ARETURN;
    }

    /**
     * Invokes the proper LDC Class Constant instructions for the given Jandex Type. This will properly create LDC instructions
     * for array types, class/parameterized classes, and primitive types by loading their equivalent <tt>TYPE</tt>
     * constants in their box types, as well as type variables (using the first bound or Object) and Void.
     * 
     * @param mv The MethodVisitor on which to visit the LDC instructions
     * @param jandexType the Jandex Type whose Class Constant to load.
     */
    public static void visitLdc(MethodVisitor mv, Type jandexType) {
        switch (jandexType.kind()) {
            case ARRAY:
                mv.visitLdcInsn(org.objectweb.asm.Type.getType(jandexType.name().toString('/').replace('.', '/')));
                break;
            case CLASS:
            case PARAMETERIZED_TYPE:
                mv.visitLdcInsn(org.objectweb.asm.Type.getType("L" + jandexType.name().toString('/') + ";"));
                break;
            case PRIMITIVE:
                switch (jandexType.asPrimitiveType().primitive()) {
                    case BOOLEAN:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                        break;
                    case BYTE:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
                        break;
                    case CHAR:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
                        break;
                    case DOUBLE:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
                        break;
                    case FLOAT:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
                        break;
                    case INT:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
                        break;
                    case LONG:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
                        break;
                    case SHORT:
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
                }
                break;
            case TYPE_VARIABLE:
                List<Type> bounds = jandexType.asTypeVariable().bounds();
                if (bounds.isEmpty())
                    mv.visitLdcInsn(org.objectweb.asm.Type.getType(Object.class));
                else
                    visitLdc(mv, bounds.get(0));
                break;
            case UNRESOLVED_TYPE_VARIABLE:
                mv.visitLdcInsn(org.objectweb.asm.Type.getType(Object.class));
                break;
            case VOID:
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
                break;
            case WILDCARD_TYPE:
                visitLdc(mv, jandexType.asWildcardType().extendsBound());
                break;
            default:
                throw new IllegalArgumentException("Unknown jandex type: " + jandexType);
        }
    }

    /**
     * Calls the right boxing method for the given Jandex Type if it is a primitive.
     * 
     * @param mv The MethodVisitor on which to visit the boxing instructions
     * @param jandexType The Jandex Type to box if it is a primitive.
     */
    public static void boxIfRequired(MethodVisitor mv, Type jandexType) {
        if (jandexType.kind() == Kind.PRIMITIVE) {
            switch (jandexType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                    break;
                case BYTE:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                    break;
                case CHAR:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
                            false);
                    break;
                case DOUBLE:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    break;
                case FLOAT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                    break;
                case INT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                    break;
                case LONG:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    break;
                case SHORT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
            }
        }
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
     * Returns the Jandex Types of the parameters of the given method descriptor.
     * 
     * @param methodDescriptor a method descriptor
     * @return the list of Jandex Type objects representing the parameters of the given method descriptor.
     */
    public static Type[] getParameterTypes(String methodDescriptor) {
        String argsSignature = methodDescriptor.substring(methodDescriptor.indexOf('(') + 1, methodDescriptor.lastIndexOf(')'));
        List<Type> args = new ArrayList<>();
        char[] chars = argsSignature.toCharArray();
        int dimensions = 0;
        int start = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case 'Z':
                    args.add(Type.create(DotName.createSimple("boolean"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'B':
                    args.add(Type.create(DotName.createSimple("byte"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'C':
                    args.add(Type.create(DotName.createSimple("char"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'D':
                    args.add(Type.create(DotName.createSimple("double"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'F':
                    args.add(Type.create(DotName.createSimple("float"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'I':
                    args.add(Type.create(DotName.createSimple("int"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'J':
                    args.add(Type.create(DotName.createSimple("long"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'S':
                    args.add(Type.create(DotName.createSimple("short"),
                            dimensions > 0 ? Kind.ARRAY : Kind.PRIMITIVE));
                    dimensions = 0;
                    start = i + 1;
                    break;
                case 'L':
                    int end = argsSignature.indexOf(';', i);
                    String binaryName = argsSignature.substring(i + 1, end);
                    // arrays take the entire signature
                    if (dimensions > 0) {
                        args.add(Type.create(DotName.createSimple(argsSignature.substring(start, end + 1).replace('/', '.')),
                                Kind.ARRAY));
                        dimensions = 0;
                    } else {
                        // class names take only the binary name
                        args.add(Type.create(DotName.createSimple(binaryName.replace('/', '.')), Kind.CLASS));
                    }
                    i = end; // we will have a ++ to get after the ;
                    start = i + 1;
                    break;
                case '[':
                    dimensions++;
                    break;
                default:
                    throw new IllegalStateException("Invalid signature char: " + c);
            }
        }
        return args.toArray(new Type[0]);
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

    /**
     * Prints the value pushed on the stack (must be an Object) by the given <tt>valuePusher</tt>
     * to STDERR.
     * 
     * @param mv The MethodVisitor to forward printing to.
     * @param valuePusher The function to invoke to push an Object to print on the stack.
     */
    public static void printValueOnStderr(MethodVisitor mv, Runnable valuePusher) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        valuePusher.run();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/Object;)V", false);
    }

    /**
     * Copy the parameter names to the given MethodVisitor, unless we don't have parameter name info
     * 
     * @param mv the visitor to copy to
     * @param method the method to copy from
     */
    public static void copyParameterNames(MethodVisitor mv, MethodInfo method) {
        int parameterSize = method.parameters().size();
        if (parameterSize > 0) {
            // perhaps we don't have parameter names
            if (method.parameterName(0) == null)
                return;
            for (int i = 0; i < parameterSize; i++) {
                mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
            }
        }
    }
}
