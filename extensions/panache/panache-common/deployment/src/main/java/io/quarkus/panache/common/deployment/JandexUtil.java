package io.quarkus.panache.common.deployment;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.impl.GenerateBridge;

public class JandexUtil {
    public static final DotName DOTNAME_GENERATE_BRIDGE = DotName.createSimple(GenerateBridge.class.getName());

    public static String getSignature(MethodInfo method, Function<String, String> typeArgMapper) {
        List<Type> parameters = method.parameters();

        StringBuilder signature = new StringBuilder("");
        for (TypeVariable typeVariable : method.typeParameters()) {
            if (signature.length() == 0)
                signature.append("<");
            else
                signature.append(",");
            signature.append(typeVariable.identifier()).append(":");
            // FIXME: only use the first bound
            toSignature(signature, typeVariable.bounds().get(0), typeArgMapper, false);
        }
        if (signature.length() > 0)
            signature.append(">");
        signature.append("(");
        for (Type type : parameters) {
            toSignature(signature, type, typeArgMapper, false);
        }
        signature.append(")");
        toSignature(signature, method.returnType(), typeArgMapper, false);
        return signature.toString();
    }

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

    static void toSignature(StringBuilder sb, Type type, Function<String, String> typeArgMapper, boolean erased) {
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
                String mappedSignature = typeArgMapper.apply(typeVariable.identifier());
                if (mappedSignature != null)
                    sb.append(mappedSignature);
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
     * Checks if the {@link ClassInfo} contains a method
     *
     * @param classInfo the {@link ClassInfo} instance
     * @param methodName the method name to check
     * @param parameters the parameter types, if any
     * @return true if the {@link ClassInfo} parameter contains this method
     */
    public static boolean containsMethod(ClassInfo classInfo,
            String methodName,
            String returnType,
            String... parameters) {
        List<Type> types = Arrays.stream(parameters).map(JandexUtil::toClassType).collect(toList());
        for (MethodInfo methodInfo : classInfo.methods()) {
            if (methodInfo.name().equals(methodName) && methodInfo.parameters().equals(types)) {
                return true;
            }
        }
        return false;
    }

    static Type toClassType(String type) {
        return Type.create(DotName.createSimple(type), Type.Kind.CLASS);
    }

    public static boolean containsMethod(ClassInfo classInfo, MethodInfo methodInfo) {
        if (classInfo.methods().contains(methodInfo)) {
            return true;
        }
        // MethodInfo may not belong to the same declaring class. Check signature
        for (MethodInfo classMethodInfo : classInfo.methods()) {
            if (classMethodInfo.name().equals(methodInfo.name()) &&
                    classMethodInfo.parameters().equals(methodInfo.parameters())) {
                return true;
            }
        }
        return false;
    }
}
