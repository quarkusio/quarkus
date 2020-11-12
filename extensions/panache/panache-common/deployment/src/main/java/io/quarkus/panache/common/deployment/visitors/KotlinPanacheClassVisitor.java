package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.deployment.util.AsmUtil.getSignature;
import static io.quarkus.deployment.util.AsmUtil.unboxIfRequired;
import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE;
import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
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

import javax.validation.constraints.NotNull;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.ClassVisitor;
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
public class KotlinPanacheClassVisitor extends ClassVisitor {
    public static final ByteCodeType OBJECT = new ByteCodeType(Object.class);
    protected static final ByteCodeType NOT_NULL = new ByteCodeType(NotNull.class);
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

    public KotlinPanacheClassVisitor(ClassVisitor outputClassVisitor, ClassInfo classInfo,
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

    public static List<ByteCodeType> findEntityTypeArguments(IndexView indexView, String repositoryClassName,
            DotName repositoryDotName) {
        for (ClassInfo classInfo : indexView.getAllKnownImplementors(repositoryDotName)) {
            if (repositoryClassName.equals(classInfo.name().toString())) {
                return recursivelyFindEntityTypeArguments(indexView, classInfo.name(), repositoryDotName);
            }
        }

        return null;
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
                            //                            definedMethods.entrySet().stream()
                            //                                    .map(e -> "\n" + e)
                            //                                    .forEach(s -> System.out.println(s));
                            throw new IllegalStateException(String.format("Should not run in to duplicate " +
                                    "mappings: \n\t%s\n\t%s\n\t%s", method, descriptor, prior));
                        }
                    });
            collectMethods(indexView.getClassByName(classInfo.superName()));
        }
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

        List<Type> parameters = method.parameters();
        AsmUtil.copyParameterNames(mv, method);
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            org.objectweb.asm.Type thisClass = getType("L" + classInfo.name().toString().replace('.', '/') + ";");
            customizer.customize(thisClass, method, mv);
        }

        addNullityChecks(mv, parameters);

        loadOperationsReference(mv);

        loadArguments(mv, parameters);
        invokeOperation(mv, method);

        mv.visitMaxs(parameters.size() + 1, parameters.size());

        for (int i = 0; i < parameters.size(); i++) {
            Type argument = parameters.get(i);
            for (AnnotationInstance annotation : argument.annotations()) {
                mv.visitParameterAnnotation(i, "L" + annotation.name() + ";", true);
            }
        }
    }

    private void generateBridge(MethodInfo method) {
        // get a bounds-erased descriptor
        String descriptor = bridgeMethodDescriptor(method, type -> {
            ByteCodeType mapped = typeArguments.get(type);
            return mapped != null ? mapped.descriptor() : type;
        });
        // make sure we need a bridge
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
            mv.visitIntInsn(Opcodes.ALOAD, i + 1);
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
        operationDescriptor = joiner + erasures.getOrDefault(key, descriptor);

        mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (returnType.kind() != Type.Kind.PRIMITIVE) {
            String cast;
            if (returnType.kind() == Type.Kind.TYPE_VARIABLE) {
                ByteCodeType type = typeArguments.getOrDefault(returnType.asTypeVariable().identifier(), entityUpperBound);
                cast = type.internalName();
            } else {
                cast = returnType.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(returnType));
    }

    private boolean isBridgeMethod(MethodInfo method) {
        return (method.flags() & ACC_BRIDGE) != ACC_BRIDGE;
    }

    private void loadArguments(MethodVisitor mv, List<Type> parameters) {
        mv.visitLdcInsn(typeArguments.get("Entity").type());
        int index = 1;
        for (Type methodParameter : parameters) {
            org.objectweb.asm.Type parameter;
            if (methodParameter.kind() == Type.Kind.TYPE_VARIABLE) {
                parameter = typeArguments.get(methodParameter.asTypeVariable().identifier()).type();
            } else {
                parameter = getType(getDescriptor(methodParameter, s -> null));
            }
            mv.visitVarInsn(parameter.getOpcode(ILOAD), index);
            // long and double take two slots and have size == 2.  others, size == 1
            index += parameter.getSize();
            if (parameter.getSort() < ARRAY) {
                org.objectweb.asm.Type wrapper = AsmUtil.autobox(parameter);
                mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                        getMethodDescriptor(wrapper, parameter), false);
            }

        }
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

    private void descriptors(MethodInfo method, StringJoiner joiner) {
        for (Type parameter : method.parameters()) {
            if (parameter.kind() == Type.Kind.TYPE_VARIABLE
                    || method.name().endsWith("ById")
                            && parameter.name().equals(typeArguments.get("Id").dotName())) {
                joiner.add(OBJECT.descriptor());
            } else {
                joiner.add(mapType(parameter));
            }
        }
    }

    private String mapType(Type parameter) {
        String descriptor;
        switch (parameter.kind()) {
            case PRIMITIVE:
            case TYPE_VARIABLE:
                descriptor = OBJECT.descriptor();
                break;
            default:
                String value = getDescriptor(parameter, argMapper);
                descriptor = erasures.getOrDefault(value, value);
        }
        return descriptor;
    }

    protected void addNullityChecks(MethodVisitor mv, List<Type> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Type parameter = parameters.get(i);
            if (parameter.hasAnnotation(NOT_NULL.dotName())) {
                mv.visitVarInsn(ALOAD, i + 1);
                mv.visitLdcInsn(parameter.name());
                mv.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull",
                        "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            }
        }
    }

    protected void loadOperationsReference(MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, typeBundle.operations().internalName(), "INSTANCE",
                typeBundle.operations().descriptor());
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

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add(classInfo.name().toString())
                .toString();
    }

    @Override
    public void visitEnd() {
        for (MethodInfo method : indexView.getClassByName(baseType.dotName()).methods()) {
            // Do not generate a method that already exists
            String descriptor = getDescriptor(method, type -> typeArguments.getOrDefault(type, OBJECT).descriptor());
            if (!definedMethods.containsKey(method.name() + descriptor)) {
                AnnotationInstance bridge = method.annotation(DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {

                    bridge.value("targetReturnTypeErased");
                    generate(method);
                    if (needsJvmBridge(method)) {
                        // if (!Modifier.isAbstract(classInfo.flags())) {
                        String bridgeDescriptor = bridgeMethodDescriptor(method, type -> {
                            ByteCodeType mapped = typeArguments.get(type);
                            return mapped != null ? mapped.descriptor() : type;
                        });
                        // make sure we need a bridge
                        if (!definedMethods.containsKey(method.name() + bridgeDescriptor)) {
                            generateBridge(method);
                        }
                        // }
                    }
                }
            }
        }

        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {

        MethodInfo methodInfo = definedMethods.entrySet().stream()
                .filter(e -> e.getKey().equals(name + descriptor))
                .map(e -> e.getValue())
                .findFirst().orElse(null);
        if (methodInfo != null && !methodInfo.hasAnnotation(DOTNAME_GENERATE_BRIDGE)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        return null;
    }
}
