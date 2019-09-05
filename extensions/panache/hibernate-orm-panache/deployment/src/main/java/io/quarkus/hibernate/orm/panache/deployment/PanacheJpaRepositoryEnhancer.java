package io.quarkus.hibernate.orm.panache.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    private final static DotName PANACHE_REPOSITORY_BINARY_NAME = DotName.createSimple(PanacheRepository.class.getName());
    private final static DotName PANACHE_REPOSITORY_BASE_BINARY_NAME = DotName
            .createSimple(PanacheRepositoryBase.class.getName());

    public PanacheJpaRepositoryEnhancer(IndexView index) {
        super(index, PanacheResourceProcessor.DOTNAME_PANACHE_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheJpaRepositoryClassVisitor(className, outputClassVisitor, panacheRepositoryBaseClassInfo,
                this.indexView);
    }

    static class PanacheJpaRepositoryClassVisitor extends PanacheRepositoryClassVisitor {

        public PanacheJpaRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo, IndexView indexView) {
            super(className, outputClassVisitor, panacheRepositoryBaseClassInfo, indexView);
        }

        @Override
        protected DotName getPanacheRepositoryDotName() {
            return PANACHE_REPOSITORY_BINARY_NAME;
        }

        @Override
        protected DotName getPanacheRepositoryBaseDotName() {
            return PANACHE_REPOSITORY_BASE_BINARY_NAME;
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return PanacheJpaEntityEnhancer.JPA_OPERATIONS_BINARY_NAME;
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

            super.visitEnd();
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            // inject Class
            mv.visitLdcInsn(entityType);
        }

        @Override
        protected String getModelDescriptor() {
            return "Ljava/lang/Class;";
        }
    }
}
