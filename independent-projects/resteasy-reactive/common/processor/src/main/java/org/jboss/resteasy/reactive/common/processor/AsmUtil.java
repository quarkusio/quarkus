package org.jboss.resteasy.reactive.common.processor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.WildcardType;

/**
 * A collection of ASM and Jandex utilities.
 * NOTE: this has a copy in AsmUtilCopy in arc-processor with some extra methods for knowing if we need a
 * signature and getting the signature of a class.
 */
public class AsmUtil {
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

}
