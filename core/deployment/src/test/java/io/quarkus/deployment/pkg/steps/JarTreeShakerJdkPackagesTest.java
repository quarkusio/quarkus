package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;

/**
 * Tests for the collection of JDK package references used by module tree shaking.
 */
class JarTreeShakerJdkPackagesTest {

    private static final String LIB = "org.lib:lib";
    private static final String EXCLUDED = "org.lib:excluded";

    @Test
    void jdkPackagesOutsideJavaPrefixAreCollected() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .dependency(LIB, generateClassReferencing("org.lib.Root", "org/w3c/dom/Document"))
                .root("org.lib.Root")
                .build();

        JarTreeShakeBuildItem result = new JarTreeShaker(input).run();

        assertThat(result.getReferencedJdkPackages()).contains("org.w3c.dom");
    }

    @Test
    void jdkPackagesOfExcludedArtifactsAreCollected() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .dependency(LIB, generateClassReferencing("org.lib.Root", "java/lang/Runnable"))
                .dependency(EXCLUDED, generateClassReferencing("org.excluded.Integrity", "java/sql/Connection"))
                .excludedArtifact(EXCLUDED)
                .root("org.lib.Root")
                .build();

        JarTreeShakeBuildItem result = new JarTreeShaker(input).run();

        assertThat(result.getReachableClassNames()).contains("org.excluded.Integrity");
        assertThat(result.getReferencedJdkPackages()).contains("java.sql");
    }

    /** Generates a class with a static method taking a parameter of the referenced type. */
    private static byte[] generateClassReferencing(String className, String referencedType) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, className.replace('.', '/'), null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "use",
                "(L" + referencedType + ";)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
