package io.quarkus.arc.processor;

import java.util.List;
import java.util.function.Function;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

/**
 * Copy of quarkus-core AsmUtil for some methods, with a tweak on the ARG_MAPPER (not name->String anymore) and
 * methods for getting a class signature and knowing if a signature is required.
 */
public class AsmUtilCopy {

    private final static Function<TypeVariable, Type> NO_ARG_MAPPER = new Function<TypeVariable, Type>() {
        @Override
        public Type apply(TypeVariable t) {
            return null;
        }
    };

    /**
     * Returns the Java bytecode signature of a given Jandex MethodInfo with no type argument mappings.
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
     * This will return <tt>&lt;R:Ljava/lang/Object;>(ITT;)Ljava/util/List&lt;TR;>;</tt>.
     * 
     * @param method the method you want the signature for.
     * @return a bytecode signature for that method.
     */
    public static String getSignature(MethodInfo method) {
        return getSignature(method, NO_ARG_MAPPER);
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
     * @param typeArgMapper a mapping between type variables and their resolved type.
     * @return a bytecode signature for that method.
     */
    public static String getSignature(MethodInfo method, Function<TypeVariable, Type> typeArgMapper) {
        List<Type> parameters = method.parameters();

        StringBuilder signature = new StringBuilder("");
        toSignature(signature, method.typeParameters(), typeArgMapper, false);
        signature.append("(");
        for (Type type : parameters) {
            toSignature(signature, type, typeArgMapper, false);
        }
        signature.append(")");
        toSignature(signature, method.returnType(), typeArgMapper, false);
        return signature.toString();
    }

    private static void toSignature(StringBuilder sb, Type type, Function<TypeVariable, Type> typeArgMapper, boolean erased) {
        switch (type.kind()) {
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                for (int i = 0; i < arrayType.dimensions(); i++)
                    sb.append("[");
                toSignature(sb, arrayType.component(), typeArgMapper, erased);
                break;
            case CLASS:
                sb.append("L");
                sb.append(type.asClassType().name().toString().replace('.', '/'));
                sb.append(";");
                break;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                sb.append("L");
                // FIXME: support owner type
                sb.append(parameterizedType.name().toString().replace('.', '/'));
                if (!erased && !parameterizedType.arguments().isEmpty()) {
                    sb.append("<");
                    List<Type> arguments = parameterizedType.arguments();
                    for (int i = 0; i < arguments.size(); i++) {
                        Type argType = arguments.get(i);
                        toSignature(sb, argType, typeArgMapper, erased);
                    }
                    sb.append(">");
                }
                sb.append(";");
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
                Type mappedType = typeArgMapper.apply(typeVariable);
                if (mappedType != null)
                    toSignature(sb, mappedType, typeArgMapper, erased);
                else if (erased)
                    toSignature(sb, typeVariable.bounds().get(0), typeArgMapper, erased);
                else
                    sb.append("T").append(typeVariable.identifier()).append(";");
                break;
            case UNRESOLVED_TYPE_VARIABLE:
                // FIXME: ??
                break;
            case VOID:
                sb.append("V");
                break;
            case WILDCARD_TYPE:
                if (!erased) {
                    sb.append("*");
                }
                break;
            default:
                break;

        }
    }

    /**
     * Returns the Java bytecode signature of a given Jandex Class using the given type argument mappings.
     *
     * For example, given this superclass:
     *
     * <pre>
     * {@code
     * public class Foo<R> extends Bar<R> implements List<String> {
     * }
     * </pre>
     *
     * This will return <tt>&lt;R:Ljava/lang/Object;>LFoo&lt;TR;>;</tt>.
     * {@code Bar} and {@code List} will be ignored, as they won't be part of the signature of the generated subclass.
     *
     * All will be as if the generated subclass was declared like this:
     *
     * <pre>
     * {@code
     * public class MyGeneratedClass<R> extends Foo<R> {
     * }
     * </pre>
     *
     * @param superClass the superclass of the type you want to generate the signature for.
     * 
     * @param superClassAsType the superclass as a Jandex Type.
     * @return a bytecode signature for that class.
     */
    public static String getGeneratedSubClassSignature(ClassInfo superClass, Type superClassAsType) {
        StringBuilder signature = new StringBuilder();
        toSignature(signature, superClass.typeParameters(), NO_ARG_MAPPER, false);
        toSignature(signature, superClassAsType, NO_ARG_MAPPER, false);
        return signature.toString();
    }

    private static void toSignature(StringBuilder sb, List<TypeVariable> typeParameters,
            Function<TypeVariable, Type> typeArgMapper, boolean b) {
        for (TypeVariable typeVariable : typeParameters) {
            if (sb.length() == 0)
                sb.append("<");
            else
                sb.append(",");
            sb.append(typeVariable.identifier()).append(":");
            // FIXME: only use the first bound
            toSignature(sb, typeVariable.bounds().get(0), typeArgMapper, false);
        }
        if (sb.length() > 0)
            sb.append(">");
    }

    /**
     * Returns true if the given method has type parameters or if its return type or parameter types require a signature
     */
    public static boolean needsSignature(MethodInfo method) {
        if (!method.typeParameters().isEmpty()
                || needsSignature(method.returnType())) {
            return true;
        }
        for (Type type : method.parameters()) {
            if (needsSignature(type))
                return true;
        }
        return false;
    }

    /**
     * Returns true if the given type contains parameterized types, type variables or wildcards
     */
    private static boolean needsSignature(Type type) {
        if (type == null) {
            return false;
        }
        switch (type.kind()) {
            case ARRAY:
            case CLASS:
            case PRIMITIVE:
            case VOID:
            default:
                return false;
            case PARAMETERIZED_TYPE:
            case TYPE_VARIABLE:
            case UNRESOLVED_TYPE_VARIABLE:
            case WILDCARD_TYPE:
                return true;
        }
    }
}
