package io.quarkus.data.hibernate.deployment;

import java.util.function.BiFunction;

import io.quarkus.deployment.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Bytecode transformer for repository interfaces that makes the entity class lookup
 * work through virtual dispatch instead of statically-bound private method calls.
 *
 * <p>
 * <b>Problem:</b> The repository interfaces declare {@code private getEntityClass()} which
 * calls {@code private doGetEntityClass()}. Since both are private, the JVM uses
 * {@code invokespecial} which is statically bound -- the call always resolves to the
 * interface's version, regardless of what the implementing class provides.
 *
 * <p>
 * <b>Solution:</b> This transformer:
 * <ol>
 * <li>Changes {@code doGetEntityClass()} from {@code ACC_PRIVATE} to {@code ACC_PUBLIC},
 * making it a virtual dispatch target</li>
 * <li>Rewrites the body of {@code getEntityClass()} to call {@code doGetEntityClass()}
 * via {@code invokeinterface} instead of {@code invokespecial}</li>
 * </ol>
 *
 * <p>
 * After transformation, the call chain becomes:
 * {@code findById() --invokespecial--> getEntityClass() --invokeinterface--> doGetEntityClass()}
 * where the last step dispatches to the impl class override provided by
 * {@link EntityClassMethodEnhancer}.
 *
 * <p>
 * Both methods remain private in the source code, so they never appear in the
 * user-facing API.
 *
 * @see EntityClassMethodEnhancer
 */
final class RepositoryInterfaceEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    static final RepositoryInterfaceEnhancer INSTANCE = new RepositoryInterfaceEnhancer();

    private static final String DO_GET_ENTITY_CLASS = "doGetEntityClass";
    private static final String GET_ENTITY_CLASS = "getEntityClass";
    private static final String DESCRIPTOR = "()Ljava/lang/Class;";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        String internalName = className.replace('.', '/');
        return new ClassVisitor(AsmUtil.ASM_API_VERSION, outputClassVisitor) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {

                if (DO_GET_ENTITY_CLASS.equals(name) && DESCRIPTOR.equals(descriptor)) {
                    // Make doGetEntityClass() public so it becomes a virtual dispatch target.
                    // The original private body (throws UnsupportedOperationException) is kept
                    // as a fallback -- it will be overridden by EntityClassMethodEnhancer on
                    // the implementing class.
                    int publicAccess = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                    return super.visitMethod(publicAccess, name, descriptor, signature, exceptions);
                }

                if (GET_ENTITY_CLASS.equals(name) && DESCRIPTOR.equals(descriptor)) {
                    // Replace the body of getEntityClass() to call doGetEntityClass() via
                    // invokeinterface instead of invokespecial. The method stays private --
                    // only its body changes.
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, internalName,
                            DO_GET_ENTITY_CLASS, DESCRIPTOR, true);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(1, 1);
                    mv.visitEnd();
                    // Return a no-op visitor to discard the original method body
                    return new MethodVisitor(Opcodes.ASM9) {
                    };
                }

                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
    }
}
