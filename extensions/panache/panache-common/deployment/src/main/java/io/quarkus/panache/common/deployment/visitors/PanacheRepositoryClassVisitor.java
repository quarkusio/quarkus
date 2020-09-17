package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.unboxIfRequired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;

public abstract class PanacheRepositoryClassVisitor extends ClassVisitor {

    protected Type entityType;
    protected String entitySignature;
    protected String entityBinaryType;
    protected String idSignature;
    protected String idBinaryType;
    protected String daoBinaryName;
    protected ClassInfo daoClassInfo;
    protected ClassInfo panacheRepositoryBaseClassInfo;
    protected IndexView indexView;
    protected Map<String, String> typeArguments = new HashMap<>();
    // set of name + "/" + descriptor
    protected Set<String> userMethods = new HashSet<>();

    public PanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor, IndexView indexView) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        daoClassInfo = indexView.getClassByName(DotName.createSimple(className));
        daoBinaryName = className.replace('.', '/');
        this.indexView = indexView;
    }

    protected abstract DotName getPanacheRepositoryDotName();

    protected abstract DotName getPanacheRepositoryBaseDotName();

    protected abstract String getPanacheOperationsInternalName();

    protected String getModelDescriptor() {
        return "Ljava/lang/Class;";
    }

    protected void injectModel(MethodVisitor mv) {
        // inject Class
        mv.visitLdcInsn(entityType);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        final String repositoryClassName = name.replace('/', '.');

        String[] foundTypeArguments = findEntityTypeArgumentsForPanacheRepository(indexView, repositoryClassName,
                getPanacheRepositoryBaseDotName());

        entityBinaryType = foundTypeArguments[0];
        entitySignature = "L" + entityBinaryType + ";";
        this.entityType = Type.getType(entitySignature);
        idBinaryType = foundTypeArguments[1];
        idSignature = "L" + idBinaryType + ";";
        Type idType = Type.getType(idSignature);

        typeArguments.put("Entity", this.entityType.getDescriptor());
        typeArguments.put("Id", idType.getDescriptor());
        this.panacheRepositoryBaseClassInfo = indexView.getClassByName(getPanacheRepositoryBaseDotName());
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        userMethods.add(methodName + "/" + descriptor);
        return super.visitMethod(access, methodName, descriptor, signature, exceptions);
    }

    public static String[] findEntityTypeArgumentsForPanacheRepository(IndexView indexView,
            String repositoryClassName,
            DotName repositoryDotName) {
        for (ClassInfo classInfo : indexView.getAllKnownImplementors(repositoryDotName)) {
            if (repositoryClassName.equals(classInfo.name().toString())) {
                return recursivelyFindEntityTypeArgumentsFromClass(indexView, classInfo.name(), repositoryDotName);
            }
        }

        return null;
    }

    public static String[] recursivelyFindEntityTypeArgumentsFromClass(IndexView indexView, DotName clazz,
            DotName repositoryDotName) {
        if (clazz.equals(JandexUtil.DOTNAME_OBJECT)) {
            return null;
        }

        List<org.jboss.jandex.Type> typeParameters = JandexUtil
                .resolveTypeParameters(clazz, repositoryDotName, indexView);
        if (typeParameters.isEmpty())
            throw new IllegalStateException(
                    "Failed to find supertype " + repositoryDotName + " from entity class " + clazz);
        org.jboss.jandex.Type entityType = typeParameters.get(0);
        org.jboss.jandex.Type idType = typeParameters.get(1);
        return new String[] {
                entityType.name().toString().replace('.', '/'),
                idType.name().toString().replace('.', '/')
        };
    }

    @Override
    public void visitEnd() {
        for (MethodInfo method : panacheRepositoryBaseClassInfo.methods()) {
            // Do not generate a method that already exists
            String descriptor = AsmUtil.getDescriptor(method, name -> typeArguments.get(name));
            if (!userMethods.contains(method.name() + "/" + descriptor)) {
                AnnotationInstance bridge = method.annotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {
                    generateModelBridge(method, bridge.value("targetReturnTypeErased"));
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
                    Type type = Type.getType(typeArguments.get(typeParamName));
                    if (type.getSort() > Type.DOUBLE) {
                        mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                    } else {
                        unboxIfRequired(mv, type);
                    }
                }
            }

            String targetDescriptor = AsmUtil.getDescriptor(method, name -> typeArguments.get(name));
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

    protected void generateModelBridge(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        String descriptor = AsmUtil.getDescriptor(method, name -> typeArguments.get(name));
        // JpaOperations erases the Id type to Object
        String descriptorForJpaOperations = AsmUtil.getDescriptor(method,
                name -> name.equals("Entity") ? entitySignature : null);
        String signature = AsmUtil.getSignature(method, name -> typeArguments.get(name));
        List<org.jboss.jandex.Type> parameters = method.parameters();

        String castTo = null;
        if (targetReturnTypeErased != null && targetReturnTypeErased.asBoolean()) {
            org.jboss.jandex.Type type = method.returnType();
            if (type.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE &&
                    type.asTypeVariable().identifier().equals("Entity")) {
                castTo = entityBinaryType;
            }
            if (castTo == null)
                castTo = type.name().toString('/');
        }

        // Note: we can't use SYNTHETIC here because otherwise Mockito will never mock these methods
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                method.name(),
                descriptor,
                signature,
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i + 1);
        }
        // inject Class
        String forwardingDescriptor = "(" + getModelDescriptor() + descriptorForJpaOperations.substring(1);
        if (castTo != null) {
            // return type is erased to Object
            int lastParen = forwardingDescriptor.lastIndexOf(')');
            forwardingDescriptor = forwardingDescriptor.substring(0, lastParen + 1) + "Ljava/lang/Object;";
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                getPanacheOperationsInternalName(),
                method.name(),
                forwardingDescriptor, false);
        if (castTo != null)
            mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
        String returnTypeDescriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        mv.visitInsn(AsmUtil.getReturnInstruction(returnTypeDescriptor));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
