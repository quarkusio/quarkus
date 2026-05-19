package io.quarkus.flyway.mongodb.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

/**
 * Rewrites the body of
 * {@code org.flywaydb.scanners.ClasspathSqlMigrationScanner#scan(Location, Configuration, ParsingContext)}
 * to delegate to {@code QuarkusMongodbPathLocationScanner.scanClasspath(...)}.
 * <p>
 * Flyway's upstream implementation calls
 * {@code Thread.currentThread().getContextClassLoader().getResource(".")} which returns
 * {@code null} under the Quarkus runtime classloader and triggers a {@link NullPointerException}
 * inside {@code matchesPath}. Replacing the method body short-circuits that scan and returns the
 * migrations already discovered at build time. The native-image equivalent is implemented as a
 * GraalVM substitution; both routes funnel through the same helper.
 */
final class ClasspathSqlMigrationScannerEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    static final String TARGET_CLASS_INTERNAL = "org/flywaydb/scanners/ClasspathSqlMigrationScanner";

    private static final String TARGET_METHOD = "scan";
    private static final String TARGET_DESCRIPTOR = "(Lorg/flywaydb/core/api/Location;"
            + "Lorg/flywaydb/core/api/configuration/Configuration;"
            + "Lorg/flywaydb/core/internal/parser/ParsingContext;)Ljava/util/Collection;";

    private static final String HELPER_CLASS_INTERNAL = "io/quarkus/flyway/mongodb/runtime/QuarkusMongodbPathLocationScanner";
    private static final String HELPER_METHOD = "scanClasspath";

    @Override
    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                if (TARGET_METHOD.equals(name) && TARGET_DESCRIPTOR.equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitVarInsn(Opcodes.ALOAD, 2);
                    mv.visitVarInsn(Opcodes.ALOAD, 3);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER_CLASS_INTERNAL, HELPER_METHOD, TARGET_DESCRIPTOR,
                            false);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
    }
}
