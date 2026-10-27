package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Tests for the collection of JDK package references used by module tree shaking.
 */
class JarTreeShakerJdkPackagesTest {

    private static final ArtifactKey LIB = ArtifactKey.ga("org.lib", "lib");
    private static final ArtifactKey EXCLUDED = ArtifactKey.ga("org.lib", "excluded");

    @Test
    void jdkPackagesOutsideJavaPrefixAreCollected() {
        Map<String, Supplier<byte[]>> depBytecode = new HashMap<>();
        Map<String, ArtifactKey> classToDep = new HashMap<>();
        addDepClass(depBytecode, classToDep, LIB, "org.lib.Root", "org/w3c/dom/Document");

        JarTreeShakeBuildItem result = run(Set.of("org.lib.Root"), depBytecode, classToDep, Set.of());

        assertThat(result.getReferencedJdkPackages()).contains("org.w3c.dom");
    }

    @Test
    void jdkPackagesOfExcludedArtifactsAreCollected() {
        Map<String, Supplier<byte[]>> depBytecode = new HashMap<>();
        Map<String, ArtifactKey> classToDep = new HashMap<>();
        addDepClass(depBytecode, classToDep, LIB, "org.lib.Root", "java/lang/Runnable");
        addDepClass(depBytecode, classToDep, EXCLUDED, "org.excluded.Integrity", "java/sql/Connection");

        JarTreeShakeBuildItem result = run(Set.of("org.lib.Root"), depBytecode, classToDep, Set.of(EXCLUDED));

        assertThat(result.getReachableClassNames()).contains("org.excluded.Integrity");
        assertThat(result.getReferencedJdkPackages()).contains("java.sql");
    }

    private static JarTreeShakeBuildItem run(Set<String> roots, Map<String, Supplier<byte[]>> depBytecode,
            Map<String, ArtifactKey> classToDep, Set<ArtifactKey> excluded) {
        JarTreeShakeInput input = new JarTreeShakeInput(
                TreeShakeConfig.TreeShakeMode.CLASSES,
                new HashSet<>(roots),
                new HashMap<>(),
                new HashSet<>(),
                depBytecode,
                new HashMap<>(),
                new HashMap<>(),
                classToDep,
                new HashMap<>(),
                new HashMap<>(),
                new HashSet<>(depBytecode.keySet()),
                21,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashSet<>(),
                new HashMap<>(),
                new HashSet<>(excluded));
        return new JarTreeShaker(input).run();
    }

    private static void addDepClass(Map<String, Supplier<byte[]>> depBytecode, Map<String, ArtifactKey> classToDep,
            ArtifactKey dep, String className, String referencedType) {
        byte[] bytecode = generateClassReferencing(className, referencedType);
        depBytecode.put(className, () -> bytecode);
        classToDep.put(className, dep);
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
