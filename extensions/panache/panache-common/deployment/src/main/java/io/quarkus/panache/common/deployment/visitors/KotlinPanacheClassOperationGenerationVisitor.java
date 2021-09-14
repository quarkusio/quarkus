package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.deployment.util.AsmUtil.getLoadOpcode;
import static io.quarkus.deployment.util.AsmUtil.getSignature;
import static io.quarkus.deployment.util.AsmUtil.unboxIfRequired;
import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static io.quarkus.panache.common.deployment.PanacheConstants.DOTNAME_GENERATE_BRIDGE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;

/**
 * kotlinc compiles default methods in to the implementing classes so we need to elide them first and then we can
 * generate new methods like we do elsewhere.
 */
public class KotlinPanacheClassOperationGenerationVisitor extends ClassVisitor {
    public static final String NOT_NULL_DESCRIPTOR = "Lorg/jetbrains/annotations/NotNull;";
    public static final String NULLABLE_DESCRIPTOR = "Lorg/jetbrains/annotations/Nullable;";
    public static final ByteCodeType OBJECT = new ByteCodeType(Object.class);
    protected static final ByteCodeType CLASS = new ByteCodeType(Class.class);
    protected final Function<String, String> argMapper;
    protected final ClassInfo classInfo;
    protected final ByteCodeType entityUpperBound;
    protected final Map<String, ByteCodeType> typeArguments = new HashMap<>();
    private final ByteCodeType baseType;
    private final Map<String, MethodInfo> definedMethods = new TreeMap<>();

    private final Map<String, String> erasures = new HashMap<>();
    private final IndexView indexView;
    protected List<PanacheMethodCustomizer> methodCustomizers;
    protected TypeBundle typeBundle;
    private final List<Label> labels = new ArrayList<>();

    public KotlinPanacheClassOperationGenerationVisitor(ClassVisitor outputClassVisitor, ClassInfo classInfo,
            IndexView indexView, TypeBundle typeBundle, ByteCodeType baseType,
            List<PanacheMethodCustomizer> methodCustomizers) {
        super(ASM_API_VERSION, outputClassVisitor);
        this.classInfo = classInfo;
        this.indexView = indexView;
        this.typeBundle = typeBundle;
        this.baseType = baseType;
        this.methodCustomizers = methodCustomizers;

        List<TypeVariable> typeVariables = indexView.getClassByName(baseType.dotName()).typeParameters();
        entityUpperBound = !typeVariables.isEmpty()
                ? new ByteCodeType(typeVariables.get(0).bounds().get(0))
                : OBJECT;

        discoverTypeParameters(classInfo, indexView, typeBundle, baseType);

        argMapper = type -> {
            ByteCodeType byteCodeType = typeArguments.get(type);
            return byteCodeType != null
                    ? byteCodeType.descriptor()
                    : type;
        };

        collectMethods(classInfo);
        filterNonOverrides();

    }

    public static List<ByteCodeType> recursivelyFindEntityTypeArguments(IndexView indexView, DotName clazz,
            DotName repositoryDotName) {
        if (clazz.equals(JandexUtil.DOTNAME_OBJECT)) {
            return Collections.emptyList();
        }

        return JandexUtil
                .resolveTypeParameters(clazz, repositoryDotName, indexView).stream()
                .map(t -> new ByteCodeType(t))
                .collect(toList());
    }

    private Label addLabel() {
        Label label = new Label();
        labels.add(label);
        return label;
    }

    protected void addNullityChecks(MethodVisitor mv, MethodInfo method) {
        int index = 1;
        for (Type methodParameter : method.parameters()) {
            org.objectweb.asm.Type parameter = asmType(methodParameter);
            if (isNotPrimitiveId(methodParameter)) {
                mv.visitVarInsn(parameter.getOpcode(ILOAD), index);
                String value = method.parameterName(index);
                mv.visitLdcInsn(value != null ? value : ("arg" + (index - 1)));
                mv.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter",
                        "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            }
            index += parameter.getSize();
        }
    }

    private void loadArguments(MethodVisitor mv, MethodInfo method) {
        mv.visitLdcInsn(typeArguments.get("Entity").type());
        int index = 1;
        for (Type methodParameter : method.parameters()) {
            org.objectweb.asm.Type parameter = asmType(methodParameter);
            mv.visitVarInsn(parameter.getOpcode(ILOAD), index);
            // long and double take two slots and have size == 2.  others, size == 1
            if (parameter.getSort() < ARRAY) {
                org.objectweb.asm.Type wrapper = AsmUtil.autobox(parameter);
                mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                        getMethodDescriptor(wrapper, parameter), false);
            } else if (parameter.getSort() == ARRAY) {
                mv.visitInsn(DUP);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOf",
                        "([Ljava/lang/Object;I)[Ljava/lang/Object;", false);
            }
            index += parameter.getSize();
        }
    }

    private void annotateParamsWithNotNull(MethodVisitor mv, MethodInfo method) {
        List<Type> parameters = method.parameters();
        if (parameters.size() != 0) {
            mv.visitAnnotableParameterCount(parameters.size(), false);
            for (int i = 0; i < parameters.size(); i++) {
                if (isNotPrimitiveId(method.parameters().get(i))) {
                    mv.visitParameterAnnotation(i, NOT_NULL_DESCRIPTOR, false);
                }
            }
        }
    }

    private boolean isNotPrimitiveId(Type type) {
        boolean primitive = true;
        if (type instanceof TypeVariable && ((TypeVariable) type).identifier().equals("Id")) {
            String identifier = ((TypeVariable) type).identifier();
            ByteCodeType idType = typeArguments.get(identifier);
            primitive = idType.descriptor().length() != 1;
        }
        return primitive;
    }

    protected String bridgeMethodDescriptor(MethodInfo method, Function<String, String> mapper) {
        StringJoiner joiner = new StringJoiner("", "(", ")");
        descriptors(method, joiner);

        AnnotationInstance annotation = method.annotation(DOTNAME_GENERATE_BRIDGE);
        boolean erased;
        if (annotation != null) {
            AnnotationValue value = annotation.value("targetReturnTypeErased");
            erased = value != null && value.asBoolean();
        } else {
            erased = false;
        }
        String returnType;
        if (erased) {
            returnType = entityUpperBound.descriptor();
        } else {
            returnType = getDescriptor(method.returnType(), mapper);
        }
        return joiner.toString() + returnType;
    }

    private void checkCast(MethodVisitor mv, Type returnType, String operationReturnType) {
        String cast;
        if (returnType.kind() == Type.Kind.TYPE_VARIABLE) {
            ByteCodeType type = typeArguments.getOrDefault(returnType.asTypeVariable().identifier(), entityUpperBound);
            cast = type.internalName();
        } else {
            cast = returnType.name().toString().replace('.', '/');
        }
        if (!cast.equals(operationReturnType)) {
            mv.visitTypeInsn(CHECKCAST, cast);
        }
    }

    private void collectMethods(ClassInfo classInfo) {
        if (classInfo != null && !classInfo.name().equals(baseType.dotName())) {
            classInfo.methods()
                    .forEach(method -> {
                        String descriptor = getDescriptor(method, m -> {
                            ByteCodeType byteCodeType = typeArguments.get(m);
                            return byteCodeType != null ? byteCodeType.descriptor() : OBJECT.descriptor();
                        });
                        MethodInfo prior = definedMethods.put(method.name() + descriptor, method);
                        if (prior != null && !isBridgeMethod(method)) {
                            throw new IllegalStateException(format("Should not run in to duplicate " +
                                    "mappings: \n\t%s\n\t%s\n\t%s", method, descriptor, prior));
                        }
                    });
            collectMethods(indexView.getClassByName(classInfo.superName()));
        }
    }

    private String desc(String name) {
        String s = name.replace(".", "/");
        s = s.startsWith("L") || s.startsWith("[") ? s : "L" + s + ";";
        return s;
    }

    private void descriptors(MethodInfo method, StringJoiner joiner) {
        ByteCodeType id = typeArguments.get("Id");
        for (Type parameter : method.parameters()) {
            if (!id.isPrimitive() && parameter.name().equals(id.dotName())) {
                joiner.add(OBJECT.descriptor());
            } else {
                joiner.add(mapType(parameter));
            }
        }
    }

    protected void discoverTypeParameters(ClassInfo classInfo, IndexView indexView, TypeBundle types, ByteCodeType baseType) {
        List<ByteCodeType> foundTypeArguments = recursivelyFindEntityTypeArguments(indexView,
                classInfo.name(), baseType.dotName());

        ByteCodeType entityType = (foundTypeArguments.size() > 0) ? foundTypeArguments.get(0) : OBJECT;
        ByteCodeType idType = (foundTypeArguments.size() > 1) ? foundTypeArguments.get(1).unbox() : OBJECT;

        typeArguments.put("Entity", entityType);
        typeArguments.put("Id", idType);
        typeArguments.keySet().stream()
                .filter(k -> !k.equals("Id"))
                .forEach(k -> erasures.put(k, OBJECT.descriptor()));
        try {
            erasures.put(typeArguments.get("Entity").dotName().toString(), entityUpperBound.descriptor());
            erasures.put(types.queryType().dotName().toString(), OBJECT.descriptor());
            erasures.put(types.updateType().dotName().toString(), OBJECT.descriptor());
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private void emitNullCheck(MethodVisitor mv, String operationDescriptor) {
        mv.visitInsn(DUP);
        mv.visitLdcInsn(elideDescriptor(operationDescriptor));
        mv.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullExpressionValue",
                "(Ljava/lang/Object;Ljava/lang/String;)V", false);
    }

    private void emitNullCheck(MethodVisitor mv, Type returnType) {
        Label label = addLabel();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, label);
        mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
        mv.visitInsn(DUP);

        ParameterizedType parameterizedType = ParameterizedType.create(returnType.name(),
                new Type[] { Type.create(typeArguments.get("Entity").dotName(), Type.Kind.CLASS) }, null);
        mv.visitLdcInsn("null cannot be cast to non-null type " + (parameterizedType.toString()
                .replace("java.util.List", "kotlin.collections.List")));

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(label);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Object" });
    }

    private String elideDescriptor(String descriptor) {
        // sample kotlinc text: "KotlinMongoOperations.IN\u2026atabase(Book::class.java)"
        if (descriptor.length() > 55) {
            return descriptor.substring(0, 24) + '\u2026' + descriptor.substring(descriptor.length() - 24);
        }
        return descriptor;
    }

    private Label endLabel() {
        return labels.get(labels.size() - 1);
    }

    private void filterNonOverrides() {
        new ArrayList<>(definedMethods.values())
                .forEach(method -> {
                    AnnotationInstance generateBridge = method.annotation(DOTNAME_GENERATE_BRIDGE);
                    if (generateBridge != null) {
                        definedMethods.remove(method.name() + getDescriptor(method, m -> m));
                        AnnotationValue typeErased = generateBridge.value("targetReturnTypeErased");
                        if (typeErased != null && typeErased.asBoolean()) {
                            definedMethods.remove(method.name()
                                    + bridgeMethodDescriptor(method, type -> type));
                        }
                    }
                });
    }

    private void generate(MethodInfo method) {
        // Note: we can't use SYNTHETIC here because otherwise Mockito will never mock these methods
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, method.name(),
                getDescriptor(method, argMapper), getSignature(method, argMapper), null);

        AsmUtil.copyParameterNames(mv, method);
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            org.objectweb.asm.Type thisClass = getType("L" + classInfo.name().toString().replace('.', '/') + ";");
            customizer.customize(thisClass, method, mv);
        }

        annotateParamsWithNotNull(mv, method);
        mv.visitCode();
        addNullityChecks(mv, method);
        loadOperationsReference(mv);
        loadArguments(mv, method);
        invokeOperation(mv, method);
        emitLocalVariablesTable(mv, method);

        mv.visitMaxs(0, 0);
    }

    private void emitLocalVariablesTable(MethodVisitor mv, MethodInfo method) {
        mv.visitLabel(addLabel());
        mv.visitLocalVariable("this", desc(classInfo.name().toString()), null, startLabel(), endLabel(), 0);
        for (int i = 0; i < method.parameters().size(); i++) {
            Type type = method.parameters().get(i);
            String typeName = type instanceof TypeVariable
                    ? this.typeArguments.get(((TypeVariable) type).identifier()).descriptor()
                    : desc(type.name().toString());
            String parameterName = method.parameterName(i);
            mv.visitLocalVariable(parameterName != null ? parameterName : ("arg" + 1), typeName, null, startLabel(),
                    endLabel(), i + 1);
        }
    }

    private void generateBridge(MethodInfo method, String descriptor) {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                method.name(),
                descriptor,
                null,
                null);
        List<Type> parameters = method.parameters();
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        // this
        mv.visitIntInsn(Opcodes.ALOAD, 0);
        // each param
        for (int i = 0; i < parameters.size(); i++) {
            Type paramType = parameters.get(i);
            if (paramType.kind() == Type.Kind.PRIMITIVE)
                throw new IllegalStateException("BUG: Don't know how to generate JVM bridge method for " + method
                        + ": has primitive parameters");
            mv.visitIntInsn(getLoadOpcode(paramType), i + 1);
            if (paramType.kind() == Type.Kind.TYPE_VARIABLE) {
                String typeParamName = paramType.asTypeVariable().identifier();
                org.objectweb.asm.Type type = getType(typeArguments.get(typeParamName).descriptor());
                if (type.getSort() > org.objectweb.asm.Type.DOUBLE) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                } else {
                    unboxIfRequired(mv, type);
                }
            }
        }

        String targetDescriptor = getDescriptor(method, name -> typeArguments.get(name).descriptor());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                classInfo.name().toString().replace('.', '/'),
                method.name(),
                targetDescriptor, false);
        String targetReturnTypeDescriptor = targetDescriptor.substring(targetDescriptor.indexOf(')') + 1);
        mv.visitInsn(AsmUtil.getReturnInstruction(targetReturnTypeDescriptor));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private void generatePrimitiveBridge(MethodInfo method, String descriptor) {
        String substring = descriptor.substring(0, descriptor.lastIndexOf(')') + 1);
        String descriptor1 = substring + OBJECT.descriptor();
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                method.name(),
                descriptor1,
                null,
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        // this
        mv.visitIntInsn(Opcodes.ALOAD, 0);
        mv.visitIntInsn(typeArguments.get("Id").type().getOpcode(ILOAD), 1);
        String targetDescriptor = getDescriptor(method, name -> typeArguments.get(name).descriptor());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                classInfo.name().toString().replace('.', '/'),
                method.name(),
                targetDescriptor, false);
        String targetReturnTypeDescriptor = targetDescriptor.substring(targetDescriptor.indexOf(')') + 1);
        mv.visitInsn(AsmUtil.getReturnInstruction(targetReturnTypeDescriptor));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private void invokeOperation(MethodVisitor mv, MethodInfo method) {
        String operationDescriptor;

        StringJoiner joiner = new StringJoiner("", "(", ")");
        joiner.add(CLASS.descriptor());
        descriptors(method, joiner);

        Type returnType = method.returnType();
        String descriptor = getDescriptor(returnType, argMapper);
        String key = returnType.kind() == Type.Kind.TYPE_VARIABLE
                ? returnType.asTypeVariable().identifier()
                : returnType.name().toString();
        String operationReturnType = erasures.getOrDefault(key, descriptor);
        operationDescriptor = joiner + operationReturnType;

        mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (returnType.kind() != Type.Kind.PRIMITIVE && returnType.kind() != Type.Kind.VOID) {
            String retType = operationReturnType.substring(1, operationReturnType.length() - 1);
            String annotationDesc = NOT_NULL_DESCRIPTOR;
            if ("findById".equals(method.name())) {
                annotationDesc = NULLABLE_DESCRIPTOR;
            } else {
                nullCheckReturn(mv, returnType,
                        typeBundle.operations().dotName().withoutPackagePrefix() + ".INSTANCE."
                                + method.name() + joiner);
            }
            checkCast(mv, returnType, retType);
            mv.visitAnnotation(annotationDesc, false);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(returnType));
    }

    private boolean isBridgeMethod(MethodInfo method) {
        return (method.flags() & ACC_BRIDGE) != ACC_BRIDGE;
    }

    private org.objectweb.asm.Type asmType(Type methodParameter) {
        org.objectweb.asm.Type parameter;
        if (methodParameter.kind() == Type.Kind.TYPE_VARIABLE) {
            parameter = typeArguments.get(methodParameter.asTypeVariable().identifier()).type();
        } else {
            parameter = getType(getDescriptor(methodParameter, s -> null));
        }
        return parameter;
    }

    protected void loadOperationsReference(MethodVisitor mv) {
        mv.visitLabel(addLabel());
        mv.visitFieldInsn(GETSTATIC, typeBundle.operations().internalName(), "INSTANCE",
                typeBundle.operations().descriptor());
    }

    private String mapType(Type parameter) {
        switch (parameter.kind()) {
            case PRIMITIVE:
            case TYPE_VARIABLE:
                return OBJECT.descriptor();
            default:
                String value = getDescriptor(parameter, argMapper);
                return erasures.getOrDefault(value, value);
        }
    }

    private boolean needsJvmBridge(MethodInfo method) {
        if (needsJvmBridge(method.returnType()))
            return true;
        for (Type paramType : method.parameters()) {
            if (needsJvmBridge(paramType))
                return true;
        }
        return false;
    }

    private boolean needsJvmBridge(Type type) {
        if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            String typeParamName = type.asTypeVariable().identifier();
            return typeArguments.containsKey(typeParamName);
        }
        return false;
    }

    private void nullCheckReturn(MethodVisitor mv, Type returnType, String operationDescriptor) {
        if (returnType instanceof ParameterizedType) {
            emitNullCheck(mv, returnType);
        } else if (returnType instanceof ClassType) {
            emitNullCheck(mv, operationDescriptor);
        }
    }

    private Label startLabel() {
        return labels.get(0);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add(classInfo.name().toString())
                .toString();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {

        MethodInfo methodInfo = definedMethods.entrySet().stream()
                .filter(e -> e.getKey().equals(name + descriptor))
                .map(e -> e.getValue())
                .findFirst()
                .orElse(null);
        if (methodInfo != null && !methodInfo.hasAnnotation(DOTNAME_GENERATE_BRIDGE)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        return null;
    }

    @Override
    public void visitEnd() {
        for (MethodInfo method : indexView.getClassByName(baseType.dotName()).methods()) {
            String descriptor = getDescriptor(method, type -> typeArguments.getOrDefault(type, OBJECT).descriptor());
            if (!definedMethods.containsKey(method.name() + descriptor)) {
                AnnotationInstance bridge = method.annotation(DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {

                    generate(method);
                    if (needsJvmBridge(method)) {
                        String bridgeDescriptor = bridgeMethodDescriptor(method, type -> {
                            ByteCodeType mapped = typeArguments.get(type);
                            return mapped != null ? mapped.descriptor() : type;
                        });
                        if (!definedMethods.containsKey(method.name() + bridgeDescriptor)) {
                            generateBridge(method, bridgeDescriptor);
                        }

                        AnnotationValue targetReturnTypeErased = bridge.value("targetReturnTypeErased");
                        if (typeArguments.get("Id").isPrimitive() && targetReturnTypeErased != null
                                && targetReturnTypeErased.asBoolean()) {
                            if (method.parameters().size() == 1
                                    && method.parameters().get(0).asTypeVariable().identifier().equals("Id")) {
                                generatePrimitiveBridge(method, descriptor);
                            }
                        }
                    }
                }
            }
        }

        super.visitEnd();
    }
}
