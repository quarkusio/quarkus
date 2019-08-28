package io.quarkus.panache.common.deployment;

import java.util.List;
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
        protected String daoBinaryName;
        protected ClassInfo panacheRepositoryBaseClassInfo;
        protected IndexView indexView;

        public PanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo, IndexView indexView) {
            super(Opcodes.ASM7, outputClassVisitor);
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

            String foundEntityType = findEntityBinaryTypeForPanacheRepository(repositoryClassName,
                    getPanacheRepositoryDotName());

            if (foundEntityType == null) {
                foundEntityType = findEntityBinaryTypeForPanacheRepository(repositoryClassName,
                        getPanacheRepositoryBaseDotName());
            }

            entityBinaryType = foundEntityType;
            entitySignature = "L" + entityBinaryType + ";";
            entityType = Type.getType(entitySignature);
        }

        private String findEntityBinaryTypeForPanacheRepository(String repositoryClassName, DotName repositoryDotName) {
            for (ClassInfo classInfo : indexView.getAllKnownImplementors(repositoryDotName)) {
                if (repositoryClassName.equals(classInfo.name().toString())) {
                    return recursivelyFindEntityTypeFromClass(classInfo.name(), repositoryDotName);
                }
            }

            return null;
        }

        private String recursivelyFindEntityTypeFromClass(DotName clazz, DotName repositoryDotName) {
            if (clazz.equals(OBJECT_DOT_NAME)) {
                return null;
            }

            final ClassInfo classByName = indexView.getClassByName(clazz);
            for (org.jboss.jandex.Type type : classByName.interfaceTypes()) {
                if (type.name().equals(repositoryDotName)) {
                    org.jboss.jandex.Type entityType = type.asParameterizedType().arguments().get(0);
                    return entityType.name().toString().replace('.', '/');
                }
            }

            return recursivelyFindEntityTypeFromClass(classByName.superName(), repositoryDotName);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {

            for (MethodInfo method : panacheRepositoryBaseClassInfo.methods()) {
                AnnotationInstance bridge = method.annotation(JandexUtil.DOTNAME_GENERATE_BRIDGE);
                if (bridge != null)
                    generateMethod(method, bridge.value("targetReturnTypeErased"));
            }

            super.visitEnd();
        }

        private void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased) {
            String descriptor = JandexUtil.getDescriptor(method, name -> name.equals("Entity") ? entitySignature : null);
            String signature = JandexUtil.getSignature(method, name -> name.equals("Entity") ? entitySignature : null);
            List<org.jboss.jandex.Type> parameters = method.parameters();

            String castTo = null;
            if (targetReturnTypeErased != null && targetReturnTypeErased.asBoolean()) {
                org.jboss.jandex.Type type = method.returnType();
                if (type.kind() == Kind.TYPE_VARIABLE) {
                    if (type.asTypeVariable().identifier().equals("Entity"))
                        castTo = entityBinaryType;
                }
                if (castTo == null)
                    castTo = type.name().toString('/');
            }

            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
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
            String forwardingDescriptor = "(" + getModelDescriptor() + descriptor.substring(1);
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
}
