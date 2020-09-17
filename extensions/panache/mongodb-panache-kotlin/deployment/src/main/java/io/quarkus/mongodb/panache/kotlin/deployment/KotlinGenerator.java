package io.quarkus.mongodb.panache.kotlin.deployment;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.deployment.util.AsmUtil.getSignature;
import static io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor.OBJECT_SIGNATURE;
import static java.util.Arrays.asList;
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
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.mongodb.panache.deployment.ByteCodeType;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;

public class KotlinGenerator {
    public static final ByteCodeType NOT_NULL = new ByteCodeType(NotNull.class);
    private static final ByteCodeType CLASS = new ByteCodeType(Class.class);

    private final Function<String, String> argMapper;
    private final ClassVisitor cv;
    private final List<MethodInfo> methods = new ArrayList<>();
    private final Map<String, String> typeArguments;
    private final TypeBundle types;
    private final Map<String, ByteCodeType> typeParameters;

    public KotlinGenerator(ClassVisitor classVisitor, Map<String, ByteCodeType> typeParameters,
            Map<String, String> typeArguments, TypeBundle types) {
        this.cv = classVisitor;
        this.typeParameters = typeParameters;
        this.typeArguments = typeArguments;
        this.types = types;
        this.argMapper = type -> typeArguments.get(type);
    }

    public static ByteCodeType[] findEntityTypeArguments(IndexView indexView, String repositoryClassName,
            DotName repositoryDotName) {
        for (ClassInfo classInfo : indexView.getAllKnownImplementors(repositoryDotName)) {
            if (repositoryClassName.equals(classInfo.name().toString())) {
                return recursivelyFindEntityTypeArguments(indexView, classInfo.name(), repositoryDotName);
            }
        }

        return null;
    }

    public static ByteCodeType[] recursivelyFindEntityTypeArguments(IndexView indexView, DotName clazz,
            DotName repositoryDotName) {
        if (clazz.equals(JandexUtil.DOTNAME_OBJECT)) {
            return null;
        }

        List<Type> typeParameters = JandexUtil
                .resolveTypeParameters(clazz, repositoryDotName, indexView);
        if (typeParameters.isEmpty())
            throw new IllegalStateException(
                    "Failed to find supertype " + repositoryDotName + " from entity class " + clazz);
        return new ByteCodeType[] {
                new ByteCodeType(typeParameters.get(0)),
                new ByteCodeType(typeParameters.get(1))
        };
    }

    public void add(MethodInfo methodInfo) {
        methods.add(methodInfo);
    }

    private void addNullityChecks(MethodVisitor mv, List<Type> parameters) {
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

    public void generate() {
        methods.forEach(m -> generate(m));
    }

    public void generate(MethodInfo method) {
        // Note: we can't use SYNTHETIC here because otherwise Mockito will never mock these methods
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, method.name(),
                getDescriptor(method, argMapper), getSignature(method, argMapper), null);

        List<Type> parameters = method.parameters();
        AsmUtil.copyParameterNames(mv, method);

        addNullityChecks(mv, parameters);

        loadOperationsReference(mv);

        loadArguments(mv, parameters);
        invokeOperation(mv, method, parameters);

        mv.visitMaxs(parameters.size() + 1, parameters.size());

        for (int i = 0; i < parameters.size(); i++) {
            Type argument = parameters.get(i);
            for (AnnotationInstance annotation : argument.annotations()) {
                mv.visitParameterAnnotation(i, "L" + annotation.name() + ";", true);
            }
        }
    }

    private void invokeOperation(MethodVisitor mv, MethodInfo method, List<Type> parameters) {
        String operationDescriptor;

        AnnotationInstance bridge = method.annotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE);
        AnnotationValue targetReturnTypeErased = bridge.value("targetReturnTypeErased");
        boolean erased = targetReturnTypeErased != null && targetReturnTypeErased.asBoolean();

        StringJoiner joiner = new StringJoiner("", "(", ")");
        joiner.add(CLASS.descriptor());
        for (Type parameter : parameters) {
            if (parameter.kind() != Type.Kind.PRIMITIVE) {
                String descriptor = getDescriptor(parameter, argMapper);
                if (typeArguments.containsValue(descriptor)) {
                    descriptor = OBJECT_SIGNATURE;
                }
                joiner.add(descriptor);
            } else {
                joiner.add(OBJECT_SIGNATURE);
            }
        }

        List<String> names = asList(types.queryType().dotName().toString(), types.updateType().dotName().toString());
        operationDescriptor = joiner +
                (erased || names.contains(method.returnType().name().toString())
                        ? OBJECT_SIGNATURE
                        : getDescriptor(method.returnType(), argMapper));

        mv.visitMethodInsn(INVOKEVIRTUAL, types.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (method.returnType().kind() != Type.Kind.PRIMITIVE) {
            Type type = method.returnType();
            String cast;
            if (type.kind() == Type.Kind.TYPE_VARIABLE) {
                String applied = argMapper.apply(type.asTypeVariable().identifier());
                cast = applied.substring(1, applied.length() - 1);
            } else {
                cast = type.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(method.returnType()));
    }

    private void loadArguments(MethodVisitor mv, List<Type> parameters) {
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(typeArguments.get("Entity")));
        for (int i = 0; i < parameters.size(); i++) {
            Type methodParameter = parameters.get(i);
            org.objectweb.asm.Type parameter;
            if (methodParameter.kind() == Type.Kind.TYPE_VARIABLE) {
                parameter = typeParameters.get(parameters.get(i).asTypeVariable().identifier()).type();
            } else {
                parameter = getType(getDescriptor(methodParameter, s -> null));
            }
            mv.visitVarInsn(parameter.getOpcode(ILOAD), i + 1);
            if (parameter.getSort() < ARRAY) {
                org.objectweb.asm.Type wrapper = AsmUtil.autobox(parameter);
                mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                        getMethodDescriptor(wrapper, parameter), false);
            }

        }
    }

    private void loadOperationsReference(MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, types.entityBase().internalName(), "Companion",
                types.entityBaseCompanion().descriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, types.entityBaseCompanion().internalName(), "getOperations",
                "()" + types.operations().descriptor(), false);
    }
}
