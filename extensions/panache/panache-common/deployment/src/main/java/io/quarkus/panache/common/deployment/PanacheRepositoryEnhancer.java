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
import org.objectweb.asm.signature.SignatureReader;

public abstract class PanacheRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    protected final ClassInfo panacheRepositoryBaseClassInfo;

    public PanacheRepositoryEnhancer(IndexView index, DotName panacheRepositoryBaseName) {
        panacheRepositoryBaseClassInfo = index.getClassByName(panacheRepositoryBaseName);
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

    protected static abstract class PanacheRepositoryClassVisitor extends ClassVisitor {

        protected Type entityType;
        protected String entitySignature;
        protected String entityBinaryType;
        protected String daoBinaryName;
        protected ClassInfo panacheRepositoryBaseClassInfo;

        public PanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo) {
            super(Opcodes.ASM7, outputClassVisitor);
            daoBinaryName = className.replace('.', '/');
            this.panacheRepositoryBaseClassInfo = panacheRepositoryBaseClassInfo;
        }

        protected abstract String getPanacheRepositoryBinaryName();

        protected abstract String getPanacheRepositoryBaseBinaryName();

        protected abstract String getPanacheOperationsBinaryName();

        protected abstract String getModelDescriptor();

        protected abstract void injectModel(MethodVisitor mv);

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(getPanacheRepositoryBinaryName());
            signatureReader.accept(daoTypeFetcher);
            if (daoTypeFetcher.foundType == null) {
                daoTypeFetcher = new DaoTypeFetcher(getPanacheRepositoryBaseBinaryName());
                signatureReader.accept(daoTypeFetcher);
            }
            entityBinaryType = daoTypeFetcher.foundType;
            entitySignature = "L" + entityBinaryType + ";";
            entityType = Type.getType(entitySignature);
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
