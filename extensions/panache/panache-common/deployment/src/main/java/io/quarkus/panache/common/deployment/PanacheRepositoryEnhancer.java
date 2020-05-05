package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

public abstract class PanacheRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {
    private static final DotName OBJECT_DOT_NAME = DotName.createSimple(Object.class.getName());

    protected final ClassInfo panacheRepositoryBaseClassInfo;
    protected final IndexView indexView;

    public PanacheRepositoryEnhancer(IndexView index, DotName panacheRepositoryBaseName) {
        panacheRepositoryBaseClassInfo = index.getClassByName(panacheRepositoryBaseName);
        this.indexView = index;
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

    protected static abstract class PanacheRepositoryClassVisitor extends ClassVisitor {

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

        public PanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo, IndexView indexView) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            daoClassInfo = indexView.getClassByName(DotName.createSimple(className));
            daoBinaryName = className.replace('.', '/');
            this.panacheRepositoryBaseClassInfo = panacheRepositoryBaseClassInfo;
            this.indexView = indexView;
        }

        protected abstract DotName getPanacheRepositoryDotName();

        protected abstract DotName getPanacheRepositoryBaseDotName();

        protected abstract String getPanacheOperationsBinaryName();

        protected abstract String getModelDescriptor();

        protected abstract void injectModel(MethodVisitor mv);

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            final String repositoryClassName = name.replace('/', '.');

            String[] foundTypeArguments = findEntityTypeArgumentsForPanacheRepository(repositoryClassName,
                    getPanacheRepositoryBaseDotName());

            entityBinaryType = foundTypeArguments[0];
            entitySignature = "L" + entityBinaryType + ";";
            entityType = Type.getType(entitySignature);
            idBinaryType = foundTypeArguments[1];
            idSignature = "L" + idBinaryType + ";";

            typeArguments.put("Entity", entitySignature);
            typeArguments.put("Id", idSignature);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            userMethods.add(methodName + "/" + descriptor);
            return super.visitMethod(access, methodName, descriptor, signature, exceptions);
        }

        private String[] findEntityTypeArgumentsForPanacheRepository(String repositoryClassName, DotName repositoryDotName) {
            for (ClassInfo classInfo : indexView.getAllKnownImplementors(repositoryDotName)) {
                if (repositoryClassName.equals(classInfo.name().toString())) {
                    return recursivelyFindEntityTypeArgumentsFromClass(classInfo.name(), repositoryDotName);
                }
            }

            return null;
        }

        private String[] recursivelyFindEntityTypeArgumentsFromClass(DotName clazz, DotName repositoryDotName) {
            if (clazz.equals(OBJECT_DOT_NAME)) {
                return null;
            }

            List<org.jboss.jandex.Type> typeParameters = io.quarkus.deployment.util.JandexUtil
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
                String descriptor = JandexUtil.getDescriptor(method, name -> typeArguments.get(name));
                if (!userMethods.contains(method.name() + "/" + descriptor)) {
                    AnnotationInstance bridge = method.annotation(JandexUtil.DOTNAME_GENERATE_BRIDGE);
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
            if (type.kind() == Kind.TYPE_VARIABLE) {
                String typeParamName = type.asTypeVariable().identifier();
                return typeArguments.containsKey(typeParamName);
            }
            return false;
        }

        private void generateJvmBridge(MethodInfo method) {
            // get a bounds-erased descriptor
            String descriptor = JandexUtil.getDescriptor(method, name -> null);
            // make sure we need a bridge
            if (!userMethods.contains(method.name() + "/" + descriptor)) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                        method.name(),
                        descriptor,
                        null,
                        null);
                List<org.jboss.jandex.Type> parameters = method.parameters();
                for (int i = 0; i < parameters.size(); i++) {
                    mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
                }
                mv.visitCode();
                // this
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                // each param
                for (int i = 0; i < parameters.size(); i++) {
                    org.jboss.jandex.Type paramType = parameters.get(i);
                    if (paramType.kind() == Kind.PRIMITIVE)
                        throw new IllegalStateException("BUG: Don't know how to generate JVM bridge method for " + method
                                + ": has primitive parameters");
                    mv.visitIntInsn(Opcodes.ALOAD, i + 1);
                    if (paramType.kind() == Kind.TYPE_VARIABLE) {
                        String typeParamName = paramType.asTypeVariable().identifier();
                        switch (typeParamName) {
                            case "Entity":
                                mv.visitTypeInsn(Opcodes.CHECKCAST, entityBinaryType);
                                break;
                            case "Id":
                                mv.visitTypeInsn(Opcodes.CHECKCAST, idBinaryType);
                                break;
                        }
                    }
                }

                String targetDescriptor = JandexUtil.getDescriptor(method, name -> typeArguments.get(name));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        daoBinaryName,
                        method.name(),
                        targetDescriptor, false);
                String targetReturnTypeDescriptor = targetDescriptor.substring(targetDescriptor.indexOf(')') + 1);
                mv.visitInsn(JandexUtil.getReturnInstruction(targetReturnTypeDescriptor));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

        }

        private void generateModelBridge(MethodInfo method, AnnotationValue targetReturnTypeErased) {
            String descriptor = JandexUtil.getDescriptor(method, name -> typeArguments.get(name));
            // JpaOperations erases the Id type to Object
            String descriptorForJpaOperations = JandexUtil.getDescriptor(method,
                    name -> name.equals("Entity") ? entitySignature : null);
            String signature = JandexUtil.getSignature(method, name -> typeArguments.get(name));
            List<org.jboss.jandex.Type> parameters = method.parameters();

            String castTo = null;
            if (targetReturnTypeErased != null && targetReturnTypeErased.asBoolean()) {
                org.jboss.jandex.Type type = method.returnType();
                if (type.kind() == Kind.TYPE_VARIABLE &&
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
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
            }
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
                    getPanacheOperationsBinaryName(),
                    method.name(),
                    forwardingDescriptor, false);
            if (castTo != null)
                mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
            String returnTypeDescriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
            mv.visitInsn(JandexUtil.getReturnInstruction(returnTypeDescriptor));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    public static boolean skipRepository(ClassInfo classInfo) {
        // we don't want to add methods to abstract/generic entities/repositories: they get added to bottom types
        // which can't be either
        return Modifier.isAbstract(classInfo.flags())
                || !classInfo.typeParameters().isEmpty();
    }
}
