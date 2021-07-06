package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.deployment.util.AsmUtil.unboxIfRequired;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor.OBJECT;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor.recursivelyFindEntityTypeArguments;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.PanacheConstants;
import io.quarkus.panache.common.deployment.TypeBundle;

public class PanacheRepositoryClassOperationGenerationVisitor extends ClassVisitor {
    public static final ByteCodeType CLASS = new ByteCodeType(Class.class);

    protected Type entityType;
    protected String entitySignature;
    protected String daoBinaryName;
    protected ClassInfo daoClassInfo;
    protected ClassInfo panacheRepositoryBaseClassInfo;
    protected IndexView indexView;
    protected Map<String, ByteCodeType> typeArguments = new HashMap<>();
    // set of name + "/" + descriptor
    protected Set<String> userMethods = new HashSet<>();
    private final TypeBundle typeBundle;
    protected Function<String, String> argMapper;
    protected ByteCodeType entityUpperBound;
    private final Map<String, String> erasures = new HashMap<>();

    public PanacheRepositoryClassOperationGenerationVisitor(String className, ClassVisitor outputClassVisitor,
            IndexView indexView,
            TypeBundle typeBundle) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        this.typeBundle = typeBundle;
        daoClassInfo = indexView.getClassByName(DotName.createSimple(className));
        daoBinaryName = className.replace('.', '/');
        this.indexView = indexView;
    }

    protected void injectModel(MethodVisitor mv) {
        // inject Class
        mv.visitLdcInsn(entityType);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        DotName baseType = typeBundle.repositoryBase().dotName();

        List<TypeVariable> typeVariables = indexView.getClassByName(baseType).typeParameters();
        entityUpperBound = !typeVariables.isEmpty()
                ? new ByteCodeType(typeVariables.get(0).bounds().get(0))
                : OBJECT;

        discoverTypeParameters(daoClassInfo, indexView, typeBundle, typeBundle.repositoryBase());

        this.entityType = typeArguments.getOrDefault("Entity", OBJECT).type();

        this.panacheRepositoryBaseClassInfo = indexView.getClassByName(baseType);

        argMapper = type -> {
            ByteCodeType byteCodeType = typeArguments.get(type);
            return byteCodeType != null
                    ? byteCodeType.descriptor()
                    : type;
        };

    }

    protected void discoverTypeParameters(ClassInfo classInfo, IndexView indexView, TypeBundle types, ByteCodeType baseType) {
        List<ByteCodeType> foundTypeArguments = recursivelyFindEntityTypeArguments(indexView,
                classInfo.name(), baseType.dotName());

        ByteCodeType entityType = (foundTypeArguments.size() > 0) ? foundTypeArguments.get(0) : OBJECT;
        ByteCodeType idType = (foundTypeArguments.size() > 1) ? foundTypeArguments.get(1) : OBJECT;

        typeArguments.put("Entity", entityType);
        typeArguments.put("Id", idType);
        typeArguments.keySet().stream()
                .filter(k -> !k.equals("Id"))
                .forEach(k -> erasures.put(k, OBJECT.descriptor()));
        try {
            ByteCodeType entity = typeArguments.get("Entity");
            erasures.put(entity.dotName().toString(), entityUpperBound.descriptor());
            erasures.put(types.queryType().dotName().toString(), OBJECT.descriptor());
            erasures.put(types.updateType().dotName().toString(), OBJECT.descriptor());
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        userMethods.add(methodName + "/" + descriptor);
        return super.visitMethod(access, methodName, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        for (MethodInfo method : panacheRepositoryBaseClassInfo.methods()) {
            // Do not generate a method that already exists
            String descriptor = getDescriptor(method, type -> typeArguments.getOrDefault(type, OBJECT).descriptor());
            if (!userMethods.contains(method.name() + "/" + descriptor)) {
                AnnotationInstance bridge = method.annotation(PanacheConstants.DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {
                    generateModelBridge(method, bridge);
                    if (needsJvmBridge(method)) {
                        generateJvmBridge(method);
                    }
                }
            }
        }
        super.visitEnd();
    }

    private boolean needsJvmBridge(MethodInfo method) {
        if (needsJvmBridge(method.returnType()))
            return true;
        for (org.jboss.jandex.Type paramType : method.parameters()) {
            if (needsJvmBridge(paramType))
                return true;
        }
        return false;
    }

    private boolean needsJvmBridge(org.jboss.jandex.Type type) {
        if (type.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
            String typeParamName = type.asTypeVariable().identifier();
            return typeArguments.containsKey(typeParamName);
        }
        return false;
    }

    protected void generateJvmBridge(MethodInfo method) {
        // get a bounds-erased descriptor
        String descriptor = AsmUtil.getDescriptor(method, name -> null);
        // make sure we need a bridge
        if (!userMethods.contains(method.name() + "/" + descriptor)) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    method.name(),
                    descriptor,
                    null,
                    null);
            List<org.jboss.jandex.Type> parameters = method.parameters();
            AsmUtil.copyParameterNames(mv, method);
            mv.visitCode();
            // this
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            // each param
            for (int i = 0; i < parameters.size(); i++) {
                org.jboss.jandex.Type paramType = parameters.get(i);
                if (paramType.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE)
                    throw new IllegalStateException("BUG: Don't know how to generate JVM bridge method for " + method
                            + ": has primitive parameters");
                mv.visitIntInsn(Opcodes.ALOAD, i + 1);
                if (paramType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
                    String typeParamName = paramType.asTypeVariable().identifier();
                    Type type = typeArguments.get(typeParamName).type();
                    if (type.getSort() > Type.DOUBLE) {
                        mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                    } else {
                        unboxIfRequired(mv, type);
                    }
                }
            }

            String targetDescriptor = AsmUtil.getDescriptor(method, argMapper);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    daoBinaryName,
                    method.name(),
                    targetDescriptor, false);
            String targetReturnTypeDescriptor = targetDescriptor.substring(targetDescriptor.indexOf(')') + 1);
            mv.visitInsn(AsmUtil.getReturnInstruction(targetReturnTypeDescriptor));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

    }

    protected void generateModelBridge(MethodInfo method, AnnotationInstance bridge) {
        // JpaOperations erases the Id type to Object
        List<org.jboss.jandex.Type> parameters = method.parameters();

        // Note: we can't use SYNTHETIC here because otherwise Mockito will never mock these methods
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                method.name(),
                AsmUtil.getDescriptor(method, argMapper),
                AsmUtil.getSignature(method, argMapper),
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        loadOperations(mv);
        boolean ignoreEntityTypeParam = isIgnoreEntityTypeParam(bridge);
        loadArguments(parameters, mv, ignoreEntityTypeParam);
        invokeOperations(mv, method, ignoreEntityTypeParam);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private boolean isIgnoreEntityTypeParam(AnnotationInstance bridge) {
        AnnotationValue ignoreEntityTypeParam = bridge.value("ignoreEntityTypeParam");
        if (ignoreEntityTypeParam == null) {
            return false; // default value
        }
        return ignoreEntityTypeParam.asBoolean();
    }

    private void invokeOperations(MethodVisitor mv, MethodInfo method, boolean ignoreEntityTypeParam) {
        String operationDescriptor;

        StringJoiner joiner = new StringJoiner("", "(", ")");
        if (!ignoreEntityTypeParam) {
            joiner.add(CLASS.descriptor());
        }
        descriptors(method, joiner);

        org.jboss.jandex.Type returnType = method.returnType();
        String descriptor = getDescriptor(returnType, argMapper);
        String key = returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                ? returnType.asTypeVariable().identifier()
                : returnType.name().toString();
        operationDescriptor = joiner + erasures.getOrDefault(key, descriptor);

        mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (returnType.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE) {
            String cast;
            if (returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
                ByteCodeType type = typeArguments.getOrDefault(returnType.asTypeVariable().identifier(), entityUpperBound);
                cast = type.internalName();
            } else {
                cast = returnType.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(returnType));
    }

    private void descriptors(MethodInfo method, StringJoiner joiner) {
        for (org.jboss.jandex.Type parameter : method.parameters()) {
            if (parameter.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                    || method.name().endsWith("ById")
                            && parameter.name().equals(typeArguments.get("Id").dotName())) {
                joiner.add(OBJECT.descriptor());
            } else {
                joiner.add(mapType(parameter));
            }
        }
    }

    private String mapType(org.jboss.jandex.Type parameter) {
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

    private void loadArguments(List<org.jboss.jandex.Type> parameters, MethodVisitor mv, boolean ignoreEntityTypeParam) {
        if (!ignoreEntityTypeParam) {
            // inject Class
            injectModel(mv);
        }
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i + 1);
        }
    }

    private void loadOperations(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, typeBundle.operations().internalName(), "INSTANCE",
                typeBundle.operations().descriptor());
    }
}
