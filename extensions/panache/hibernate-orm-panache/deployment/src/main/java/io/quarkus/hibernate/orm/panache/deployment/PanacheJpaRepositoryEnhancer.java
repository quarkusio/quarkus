package io.quarkus.hibernate.orm.panache.deployment;

import static io.quarkus.hibernate.orm.panache.deployment.PanacheJpaEntityEnhancer.JPA_OPERATIONS_BINARY_NAME;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.deployment.JandexUtil;

public class PanacheJpaRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String PANACHE_REPOSITORY_BASE_NAME = PanacheRepositoryBase.class.getName();
    public final static String PANACHE_REPOSITORY_BASE_BINARY_NAME = PANACHE_REPOSITORY_BASE_NAME.replace('.', '/');

    public final static String PANACHE_REPOSITORY_NAME = PanacheRepository.class.getName();
    public final static String PANACHE_REPOSITORY_BINARY_NAME = PANACHE_REPOSITORY_NAME.replace('.', '/');
    private final ClassInfo panacheRepositoryBaseClassInfo;

    public PanacheJpaRepositoryEnhancer(IndexView index) {
        panacheRepositoryBaseClassInfo = index.getClassByName(PanacheResourceProcessor.DOTNAME_PANACHE_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DaoEnhancingClassVisitor(className, outputClassVisitor, panacheRepositoryBaseClassInfo);
    }

    static class DaoEnhancingClassVisitor extends ClassVisitor {

        private Type entityType;
        private String entitySignature;
        private String entityBinaryType;
        private String daoBinaryName;
        private ClassInfo panacheRepositoryBaseClassInfo;

        public DaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo) {
            super(Opcodes.ASM7, outputClassVisitor);
            daoBinaryName = className.replace('.', '/');
            this.panacheRepositoryBaseClassInfo = panacheRepositoryBaseClassInfo;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(PANACHE_REPOSITORY_BINARY_NAME);
            signatureReader.accept(daoTypeFetcher);
            if (daoTypeFetcher.foundType == null) {
                daoTypeFetcher = new DaoTypeFetcher(PANACHE_REPOSITORY_BASE_BINARY_NAME);
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

            // Bridge for findById
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    "findById",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    null,
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    daoBinaryName,
                    "findById",
                    "(Ljava/lang/Object;)" + entitySignature, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

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
            // inject Class
            mv.visitLdcInsn(entityType);
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i + 1);
            }
            // inject Class
            String forwardingDescriptor = "(Ljava/lang/Class;" + descriptor.substring(1);
            if (castTo != null) {
                // return type is erased to Object
                int lastParen = forwardingDescriptor.lastIndexOf(')');
                forwardingDescriptor = forwardingDescriptor.substring(0, lastParen + 1) + "Ljava/lang/Object;";
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
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
