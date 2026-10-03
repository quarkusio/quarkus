package io.quarkus.data.hibernate.deployment;

import java.util.function.BiFunction;

import io.quarkus.deployment.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Adds a {@code doGetEntityClass()} method to generated repository implementation
 * classes, returning the concrete entity class known at build time.
 *
 * <p>
 * This is the final piece of the entity class lookup fix. The method overrides the
 * default from the (now-public) {@code doGetEntityClass()} declared in the repository
 * interfaces. When ArC generates a {@code _Subclass} for intercepted repositories,
 * this override is inherited, so the entity class is always resolved correctly.
 *
 * @see RepositoryInterfaceEnhancer
 * @see RepositoryBaseDiamondResolver
 */
final class EntityClassMethodEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private final String entityInternalName;

    EntityClassMethodEnhancer(String entityInternalName) {
        this.entityInternalName = entityInternalName;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ClassVisitor(AsmUtil.ASM_API_VERSION, outputClassVisitor) {
            @Override
            public void visitEnd() {
                MethodVisitor mv = super.visitMethod(
                        Opcodes.ACC_PUBLIC, "doGetEntityClass", "()Ljava/lang/Class;", null, null);
                mv.visitCode();
                mv.visitLdcInsn(Type.getObjectType(entityInternalName));
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
                super.visitEnd();
            }
        };
    }
}
