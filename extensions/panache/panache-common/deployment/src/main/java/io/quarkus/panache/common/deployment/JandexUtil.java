package io.quarkus.panache.common.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.MethodVisitor;
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

    public static String getDescriptor(Type type, Function<String, String> typeArgMapper) {
        StringBuilder sb = new StringBuilder();
        toSignature(sb, type, typeArgMapper, true);
        return sb.toString();
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

    private static void unbox(MethodVisitor mv, String owner, String methodName, String returnTypeSignature) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, owner);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, methodName, "()" + returnTypeSignature, false);
    }

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
                        args.add(Type.create(DotName.createSimple(argsSignature.substring(start, end + 1)), Kind.ARRAY));
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
