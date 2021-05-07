package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor.OBJECT;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor.recursivelyFindEntityTypeArguments;
import static io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor.CLASS;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Modifier;
import java.util.Arrays;
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
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerVisitor;
import io.quarkus.panache.common.deployment.TypeBundle;

/**
 * A visitor that adds Panache operations to a Panache entity type.
 */
public class PanacheEntityClassOperationGenerationVisitor extends ClassVisitor {

    protected Type thisClass;
    private final Set<String> userMethods = new HashSet<>();
    protected TypeBundle typeBundle;
    protected final ClassInfo panacheEntityBaseClassInfo;
    protected ClassInfo entityInfo;
    protected List<PanacheMethodCustomizer> methodCustomizers;
    protected final Map<String, ByteCodeType> typeArguments = new HashMap<>();
    protected final Function<String, String> argMapper;
    protected final ByteCodeType entityUpperBound;
    private final Map<String, String> erasures = new HashMap<>();

    public PanacheEntityClassOperationGenerationVisitor(ClassVisitor outputClassVisitor,
            TypeBundle typeBundle,
            ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers, IndexView indexView) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);

        String className = entityInfo.name().toString();
        thisClass = Type.getType("L" + className.replace('.', '/') + ";");
        this.typeBundle = typeBundle;
        this.panacheEntityBaseClassInfo = indexView.getClassByName(typeBundle.entityBase().dotName());
        this.entityInfo = entityInfo;
        this.methodCustomizers = methodCustomizers;

        ByteCodeType baseType = typeBundle.entityBase();
        List<TypeVariable> typeVariables = indexView.getClassByName(baseType.dotName()).typeParameters();
        if (!typeVariables.isEmpty()) {
            entityUpperBound = new ByteCodeType(typeVariables.get(0).bounds().get(0));
        } else {
            entityUpperBound = null;
        }

        discoverTypeParameters(entityInfo, indexView, typeBundle, baseType);
        argMapper = type -> {
            ByteCodeType byteCodeType = typeArguments.get(type);
            return byteCodeType != null
                    ? byteCodeType.descriptor()
                    : OBJECT.descriptor();
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        userMethods.add(methodName + "/" + descriptor);
        MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
        if (Modifier.isStatic(access)
                && Modifier.isPublic(access)
                && (access & Opcodes.ACC_SYNTHETIC) == 0
                && !methodCustomizers.isEmpty()) {
            org.jboss.jandex.Type[] argTypes = AsmUtil.getParameterTypes(descriptor);
            MethodInfo method = this.entityInfo.method(methodName, argTypes);
            if (method == null) {
                throw new IllegalStateException(
                        "Could not find indexed method: " + thisClass + "." + methodName + " with descriptor " + descriptor
                                + " and arg types " + Arrays.toString(argTypes));
            }
            superVisitor = new PanacheMethodCustomizerVisitor(superVisitor, method, thisClass, methodCustomizers);
        }
        return superVisitor;
    }

    @Override
    public void visitEnd() {
        // FIXME: generate default constructor

        for (MethodInfo method : panacheEntityBaseClassInfo.methods()) {
            // Do not generate a method that already exists
            String descriptor = AsmUtil.getDescriptor(method, name -> null);
            if (!userMethods.contains(method.name() + "/" + descriptor)) {
                AnnotationInstance bridge = method.annotation(PanacheConstants.DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {
                    generateMethod(method, bridge.value("targetReturnTypeErased"), bridge.value("callSuperMethod"));
                }
            }
        }

        super.visitEnd();
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
            if (entity != null) {
                erasures.put(entity.dotName().toString(), entity.descriptor());
            }
            erasures.put(types.queryType().dotName().toString(), OBJECT.descriptor());
            erasures.put(types.updateType().dotName().toString(), OBJECT.descriptor());
        } catch (UnsupportedOperationException ignored) {
        }
    }

    protected void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased, AnnotationValue callSuperMethod) {
        List<org.jboss.jandex.Type> parameters = method.parameters();

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                method.name(),
                AsmUtil.getDescriptor(method, name -> null),
                AsmUtil.getSignature(method, name1 -> null),
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            customizer.customize(thisClass, method, mv);
        }
        if (callSuperMethod != null && callSuperMethod.asBoolean()) {
            // delegate to super method
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i);
            }
            invokeOperations(mv, method, true);
        } else {
            loadOperations(mv);
            loadArguments(mv, parameters);
            invokeOperations(mv, method, false);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void loadOperations(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, typeBundle.operations().internalName(), "INSTANCE",
                typeBundle.operations().descriptor());
    }

    private void loadArguments(MethodVisitor mv, List<org.jboss.jandex.Type> parameters) {
        // inject Class
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i);
        }
    }

    private void invokeOperations(MethodVisitor mv, MethodInfo method, boolean callSuperMethod) {
        String operationDescriptor;

        StringJoiner joiner = new StringJoiner("", "(", ")");
        if (!callSuperMethod) {
            joiner.add(CLASS.descriptor());
        }
        descriptors(method, joiner);

        org.jboss.jandex.Type returnType = method.returnType();
        String descriptor = getDescriptor(returnType, argMapper);
        String key = returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                ? returnType.asTypeVariable().identifier()
                : returnType.name().toString();
        operationDescriptor = joiner + erasures.getOrDefault(key, descriptor);

        if (callSuperMethod) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, typeBundle.entityBase().internalName(), method.name(),
                    operationDescriptor, false);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                    operationDescriptor, false);
        }
        if (returnType.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE
                && returnType.kind() != org.jboss.jandex.Type.Kind.VOID) {
            String cast;
            if (returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
                TypeVariable typeVariable = returnType.asTypeVariable();
                ByteCodeType type = typeArguments.get(typeVariable.identifier());
                if (type == null && typeVariable.bounds().size() != 1) {
                    type = OBJECT;
                } else {
                    type = new ByteCodeType(typeVariable.bounds().get(0));
                }
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

    protected void injectModel(MethodVisitor mv) {
        mv.visitLdcInsn(thisClass);
    }

}
