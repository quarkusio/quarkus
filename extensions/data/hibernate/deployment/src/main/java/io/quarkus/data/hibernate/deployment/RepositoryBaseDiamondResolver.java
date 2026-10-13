package io.quarkus.data.hibernate.deployment;

import java.util.function.BiFunction;

import io.quarkus.deployment.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Resolves the diamond inheritance of {@code doGetEntityClass()} in Base interfaces.
 *
 * <p>
 * Each Base interface (e.g. {@code BlockingManagedRepositoryBase}) extends both an
 * Operations and a Queries interface. After {@link RepositoryInterfaceEnhancer} makes
 * {@code doGetEntityClass()} public in both parents, the JVM requires the Base interface
 * to explicitly select one -- otherwise it throws {@code IncompatibleClassChangeError}.
 *
 * <p>
 * This transformer adds a public {@code doGetEntityClass()} that throws
 * {@code UnsupportedOperationException}. The method is never actually called on the
 * interface -- it exists solely to satisfy the JVM's diamond resolution requirement.
 * The real implementation lives on the generated repository class via
 * {@link EntityClassMethodEnhancer}.
 *
 * @see RepositoryInterfaceEnhancer
 * @see EntityClassMethodEnhancer
 */
final class RepositoryBaseDiamondResolver implements BiFunction<String, ClassVisitor, ClassVisitor> {

    static final RepositoryBaseDiamondResolver INSTANCE = new RepositoryBaseDiamondResolver();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ClassVisitor(AsmUtil.ASM_API_VERSION, outputClassVisitor) {
            @Override
            public void visitEnd() {
                MethodVisitor mv = super.visitMethod(
                        Opcodes.ACC_PUBLIC, "doGetEntityClass", "()Ljava/lang/Class;", null, null);
                mv.visitCode();
                mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException");
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn("doGetEntityClass() should be provided by the generated repository implementation");
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException",
                        "<init>", "(Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.ATHROW);
                mv.visitMaxs(3, 1);
                mv.visitEnd();
                super.visitEnd();
            }
        };
    }
}
