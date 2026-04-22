package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TreeShakeIT extends MojoTestBase {

    private File libsDir;
    private File appDir;

    @BeforeAll
    void installLibs() throws Exception {
        libsDir = initProject("projects/tree-shake/libs");
        generateSerializedResource(libsDir);
        RunningInvoker libsInvoker = new RunningInvoker(libsDir, false);
        MavenProcessInvocationResult result = libsInvoker.execute(
                List.of("clean install -DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(libsInvoker.log()).containsIgnoringCase("BUILD SUCCESS");

        appDir = initProject("projects/tree-shake/app");
    }

    /**
     * Generates {@code lib-serialization/src/main/resources/org/acme/serialization/data.ser}
     * containing a Java serialization stream with a class descriptor for
     * {@code org.acme.serialization.SerializedTarget}. This class has no direct bytecode
     * reference from application code — the tree shaker must discover it by scanning
     * the serialized resource.
     *
     * <p>
     * The stream is written manually so the class descriptor contains the correct
     * class name without needing the target class on the test classpath.
     */
    private static void generateSerializedResource(File libsDir) throws IOException {
        File resourceDir = new File(libsDir,
                "lib-serialization/src/main/resources/org/acme/serialization");
        resourceDir.mkdirs();
        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(new File(resourceDir, "data.ser")))) {
            dos.writeShort(0xACED); // magic
            dos.writeShort(0x0005); // version
            dos.writeByte(0x73); // TC_OBJECT
            dos.writeByte(0x72); // TC_CLASSDESC
            dos.writeUTF("org.acme.serialization.SerializedTarget"); // class name
            dos.writeLong(1L); // serialVersionUID
            dos.writeByte(0x02); // SC_SERIALIZABLE
            dos.writeShort(1); // field count = 1
            dos.writeByte('L'); // field type: object
            dos.writeUTF("name"); // field name
            dos.writeByte(0x74); // TC_STRING (field class name)
            dos.writeUTF("Ljava/lang/String;");
            dos.writeByte(0x78); // TC_ENDBLOCKDATA (classAnnotation)
            dos.writeByte(0x70); // TC_NULL (superClassDesc)
            dos.writeByte(0x74); // TC_STRING (field value)
            dos.writeUTF("deserialized");
        }
    }

    // --- Helper methods ---

    private File findJar(File libDir, String artifactId) {
        String expectedName = "org.acme.treeshake." + artifactId + "-1.0-SNAPSHOT.jar";
        File jar = new File(libDir, expectedName);
        if (!jar.exists()) {
            // legacy-jar uses "modified-" prefix for JARs with transformed classes
            jar = new File(libDir, "modified-" + expectedName);
        }
        assertThat(jar).as("JAR for " + artifactId).exists();
        return jar;
    }

    private void assertJarContains(File libDir, String artifactId, String classPath) throws IOException {
        try (JarFile jar = new JarFile(findJar(libDir, artifactId))) {
            assertThat(jar.getJarEntry(classPath))
                    .as(classPath + " in " + artifactId).isNotNull();
        }
    }

    private void assertJarNotContains(File libDir, String artifactId, String classPath) throws IOException {
        try (JarFile jar = new JarFile(findJar(libDir, artifactId))) {
            assertThat(jar.getJarEntry(classPath))
                    .as(classPath + " should not be in " + artifactId).isNull();
        }
    }

    private void assertJarHasNoAppClasses(File libDir, String artifactId) throws IOException {
        try (JarFile jar = new JarFile(findJar(libDir, artifactId))) {
            long classCount = jar.stream()
                    .filter(e -> e.getName().endsWith(".class") && !e.getName().equals("module-info.class"))
                    .count();
            assertThat(classCount).as("classes in " + artifactId).isZero();
        }
    }

    private void assertUberJarContains(File uberJar, String classPath) throws IOException {
        try (JarFile jar = new JarFile(uberJar)) {
            assertThat(jar.getJarEntry(classPath))
                    .as(classPath + " in uber-jar").isNotNull();
        }
    }

    private void assertUberJarNotContains(File uberJar, String classPath) throws IOException {
        try (JarFile jar = new JarFile(uberJar)) {
            assertThat(jar.getJarEntry(classPath))
                    .as(classPath + " should not be in uber-jar").isNull();
        }
    }

    private void verifyAllEdgeCases(File libDir) throws IOException {
        verifyAllEdgeCases(libDir, true);
    }

    private void verifyAllEdgeCases(File libDir, boolean checkTransformsInLib) throws IOException {
        // Basic reachability: lib-a/b/c/d
        assertJarContains(libDir, "lib-a", "org/acme/liba/UsedByB.class");
        assertJarContains(libDir, "lib-a", "org/acme/liba/UsedByD.class");
        assertJarContains(libDir, "lib-a", "org/acme/liba/UsedByBoth.class");
        assertJarNotContains(libDir, "lib-a", "org/acme/liba/UnusedA.class");

        assertJarContains(libDir, "lib-b", "org/acme/libb/ServiceB.class");
        assertJarContains(libDir, "lib-b", "org/acme/libb/HelperB.class");
        assertJarNotContains(libDir, "lib-b", "org/acme/libb/UnusedB.class");

        assertJarContains(libDir, "lib-c", "org/acme/libc/ServiceC.class");
        assertJarNotContains(libDir, "lib-c", "org/acme/libc/UnusedC.class");

        assertJarContains(libDir, "lib-d", "org/acme/libd/ServiceD.class");
        assertJarContains(libDir, "lib-d", "org/acme/libd/HelperD.class");
        assertJarNotContains(libDir, "lib-d", "org/acme/libd/UnusedD.class");

        // Entire dependency stripped
        assertJarHasNoAppClasses(libDir, "lib-e");

        // Annotations
        assertJarContains(libDir, "lib-annotations", "org/acme/annotations/AnnotationType.class");
        assertJarContains(libDir, "lib-annotations", "org/acme/annotations/AnnotationValue.class");
        assertJarNotContains(libDir, "lib-annotations", "org/acme/annotations/UnusedAnno.class");

        // Field annotations
        assertJarContains(libDir, "lib-field-annotations", "org/acme/fieldannos/FieldAnnoType.class");
        assertJarContains(libDir, "lib-field-annotations", "org/acme/fieldannos/FieldAnnoValue.class");
        assertJarContains(libDir, "lib-field-annotations", "org/acme/fieldannos/FieldAnnoHolder.class");
        assertJarNotContains(libDir, "lib-field-annotations", "org/acme/fieldannos/UnusedFieldAnno.class");

        // Generics
        assertJarContains(libDir, "lib-generics", "org/acme/generics/GenericContainer.class");
        assertJarContains(libDir, "lib-generics", "org/acme/generics/GenericArg.class");
        assertJarNotContains(libDir, "lib-generics", "org/acme/generics/UnusedGeneric.class");

        // Method descriptors
        assertJarContains(libDir, "lib-method-descriptors", "org/acme/descriptors/DescriptorService.class");
        assertJarContains(libDir, "lib-method-descriptors", "org/acme/descriptors/ParamType.class");
        assertJarContains(libDir, "lib-method-descriptors", "org/acme/descriptors/ReturnType.class");
        assertJarNotContains(libDir, "lib-method-descriptors", "org/acme/descriptors/UnusedDescriptor.class");

        // Inner classes
        assertJarContains(libDir, "lib-inner-classes", "org/acme/inner/Outer.class");
        assertJarContains(libDir, "lib-inner-classes", "org/acme/inner/Outer$Inner.class");
        assertJarNotContains(libDir, "lib-inner-classes", "org/acme/inner/UnusedInner.class");

        // ServiceLoader
        assertJarContains(libDir, "lib-serviceloader", "org/acme/serviceloader/ServiceInterface.class");
        assertJarContains(libDir, "lib-serviceloader", "org/acme/serviceloader/ServiceProvider.class");
        assertJarNotContains(libDir, "lib-serviceloader", "org/acme/serviceloader/UnusedProvider.class");

        // Class.forName
        assertJarContains(libDir, "lib-class-forname", "org/acme/forname/ForNameTarget.class");
        assertJarNotContains(libDir, "lib-class-forname", "org/acme/forname/UnusedForName.class");

        // ClassLoader.loadClass
        assertJarContains(libDir, "lib-classloader-load", "org/acme/loadclass/LoadClassTarget.class");
        assertJarNotContains(libDir, "lib-classloader-load", "org/acme/loadclass/UnusedLoadClass.class");

        // String constants
        assertJarContains(libDir, "lib-string-constants", "org/acme/stringconst/StringRefTarget.class");
        assertJarNotContains(libDir, "lib-string-constants", "org/acme/stringconst/UnusedStringRef.class");

        // Delimited lists
        assertJarContains(libDir, "lib-delimited-lists", "org/acme/delimited/ListTargetA.class");
        assertJarContains(libDir, "lib-delimited-lists", "org/acme/delimited/ListTargetB.class");
        assertJarNotContains(libDir, "lib-delimited-lists", "org/acme/delimited/UnusedListTarget.class");

        // Multi-release JAR: reachable class and its versioned entries are preserved
        assertJarContains(libDir, "lib-multirelease", "org/acme/multirelease/MultiReleaseClass.class");
        assertJarContains(libDir, "lib-multirelease",
                "META-INF/versions/11/org/acme/multirelease/MultiReleaseClass.class");
        assertJarContains(libDir, "lib-multirelease",
                "META-INF/versions/99/org/acme/multirelease/MultiReleaseClass.class");
        // Unreachable class: base and all versioned entries are removed
        assertJarNotContains(libDir, "lib-multirelease", "org/acme/multirelease/UnusedMultiRelease.class");
        assertJarNotContains(libDir, "lib-multirelease",
                "META-INF/versions/11/org/acme/multirelease/UnusedMultiRelease.class");
        assertJarNotContains(libDir, "lib-multirelease",
                "META-INF/versions/99/org/acme/multirelease/UnusedMultiRelease.class");
        // Higher-version-only classes referenced by reachable higher-version code are preserved,
        // along with classes they transitively reference
        assertJarContains(libDir, "lib-multirelease",
                "META-INF/versions/99/org/acme/multirelease/FutureVersionOnly.class");
        assertJarContains(libDir, "lib-multirelease", "org/acme/multirelease/FutureVersionDep.class");

        // JBoss Logging companions
        assertJarContains(libDir, "lib-jboss-logging", "org/acme/logging/LoggedClass.class");
        assertJarContains(libDir, "lib-jboss-logging", "org/acme/logging/LoggedClass_$logger.class");
        assertJarContains(libDir, "lib-jboss-logging", "org/acme/logging/LoggedClass_$bundle.class");
        assertJarNotContains(libDir, "lib-jboss-logging", "org/acme/logging/UnusedLogged.class");

        // Throws clause and catch blocks
        assertJarContains(libDir, "lib-throws", "org/acme/throws_/CustomException.class");
        assertJarContains(libDir, "lib-throws", "org/acme/throws_/CaughtException.class");
        assertJarContains(libDir, "lib-throws", "org/acme/throws_/ThrowingService.class");
        assertJarNotContains(libDir, "lib-throws", "org/acme/throws_/UnusedThrows.class");

        // Field types
        assertJarContains(libDir, "lib-field-types", "org/acme/fieldtypes/FieldType.class");
        assertJarContains(libDir, "lib-field-types", "org/acme/fieldtypes/FieldHolder.class");
        assertJarNotContains(libDir, "lib-field-types", "org/acme/fieldtypes/UnusedFieldType.class");

        // InvokeDynamic
        assertJarContains(libDir, "lib-invokedynamic", "org/acme/invokedyn/LambdaTarget.class");
        assertJarNotContains(libDir, "lib-invokedynamic", "org/acme/invokedyn/UnusedInvokeDynamic.class");

        // Sisu named components
        assertJarContains(libDir, "lib-sisu", "org/acme/sisu/SisuNamedComponent.class");
        assertJarNotContains(libDir, "lib-sisu", "org/acme/sisu/UnusedSisu.class");

        // Reflection-registered classes (via @RegisterForReflection)
        assertJarContains(libDir, "lib-reflection", "org/acme/libreflection/ReflectionTarget.class");
        assertJarContains(libDir, "lib-reflection", "org/acme/libreflection/ReflectionDep.class");
        assertJarNotContains(libDir, "lib-reflection", "org/acme/libreflection/UnusedReflection.class");

        // JNI-registered classes (via JniRuntimeAccessBuildItem)
        assertJarContains(libDir, "lib-jni", "org/acme/libjni/JniTarget.class");
        assertJarContains(libDir, "lib-jni", "org/acme/libjni/JniDep.class");
        assertJarNotContains(libDir, "lib-jni", "org/acme/libjni/UnusedJni.class");

        // Serialized resource classes (ObjectInputStream + getResource pattern)
        assertJarContains(libDir, "lib-serialization", "org/acme/serialization/SerializedTarget.class");
        assertJarContains(libDir, "lib-serialization", "org/acme/serialization/ResourceDeserializer.class");
        assertJarNotContains(libDir, "lib-serialization", "org/acme/serialization/UnusedSerialization.class");

        // Class-loading chain analysis (runtime-constructed class names + Class.forName)
        assertJarContains(libDir, "lib-loadclass-chain", "org/acme/loadchain/AlphaTarget.class");
        assertJarContains(libDir, "lib-loadclass-chain", "org/acme/loadchain/BetaTarget.class");
        assertJarContains(libDir, "lib-loadclass-chain", "org/acme/loadchain/ChainProvider.class");
        assertJarNotContains(libDir, "lib-loadclass-chain", "org/acme/loadchain/UnusedChain.class");

        // Transformed classes
        // In fast-jar, originals stay in lib JAR (transforms go to transformed-bytecode.jar).
        // In legacy-jar, transformed classes are moved to the runner JAR, so they're not in the lib JAR.
        if (checkTransformsInLib) {
            assertJarContains(libDir, "lib-transform", "org/acme/transform/TransformableClass.class");
            assertJarContains(libDir, "lib-transform", "org/acme/transform/TransformAddedRef.class");
        }
        assertJarNotContains(libDir, "lib-transform", "org/acme/transform/UnusedTransform.class");
    }

    // --- Test methods ---

    @Test
    void testFastJar() throws Exception {
        RunningInvoker appInvoker = new RunningInvoker(appDir, false);
        MavenProcessInvocationResult result = appInvoker.execute(
                List.of("clean verify"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(appInvoker.log()).containsIgnoringCase("BUILD SUCCESS");

        File fastJarLibDir = new File(appDir, "target/quarkus-app/lib/main");
        assertThat(fastJarLibDir).isDirectory();

        verifyAllEdgeCases(fastJarLibDir);
    }

    @Test
    void testLegacyJar() throws Exception {
        RunningInvoker legacyInvoker = new RunningInvoker(appDir, false);
        Properties legacyProps = new Properties();
        legacyProps.setProperty("quarkus.package.jar.type", "legacy-jar");
        MavenProcessInvocationResult result = legacyInvoker.execute(
                List.of("clean verify"),
                Map.of(), legacyProps);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(legacyInvoker.log()).containsIgnoringCase("BUILD SUCCESS");

        File legacyLibDir = new File(appDir, "target/lib");
        assertThat(legacyLibDir).isDirectory();

        verifyAllEdgeCases(legacyLibDir, false);

        // In legacy-jar, transformed classes are moved from lib JARs to the runner JAR.
        // TransformAddedRef is not itself transformed — it's only referenced by the transformation,
        // so it stays in the lib JAR.
        assertJarNotContains(legacyLibDir, "lib-transform", "org/acme/transform/TransformableClass.class");
        assertJarContains(legacyLibDir, "lib-transform", "org/acme/transform/TransformAddedRef.class");

        File[] runnerJars = new File(appDir, "target").listFiles(
                f -> f.getName().endsWith("-runner.jar"));
        assertThat(runnerJars).hasSize(1);
        File runnerJar = runnerJars[0];
        assertUberJarContains(runnerJar, "org/acme/transform/TransformableClass.class");
    }

    @Test
    void testUberJar() throws Exception {
        RunningInvoker uberInvoker = new RunningInvoker(appDir, false);
        Properties uberProps = new Properties();
        uberProps.setProperty("quarkus.package.jar.type", "uber-jar");
        MavenProcessInvocationResult result = uberInvoker.execute(
                List.of("clean verify"),
                Map.of(), uberProps);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(uberInvoker.log()).containsIgnoringCase("BUILD SUCCESS");

        File[] uberJars = new File(appDir, "target").listFiles(
                f -> f.getName().endsWith("-runner.jar"));
        assertThat(uberJars).hasSize(1);
        File uberJarFile = uberJars[0];

        // Spot-check key cases in uber-jar
        assertUberJarContains(uberJarFile, "org/acme/liba/UsedByB.class");
        assertUberJarNotContains(uberJarFile, "org/acme/liba/UnusedA.class");
        assertUberJarContains(uberJarFile, "org/acme/libb/ServiceB.class");
        assertUberJarNotContains(uberJarFile, "org/acme/libb/UnusedB.class");
        assertUberJarNotContains(uberJarFile, "org/acme/libe/OrphanE1.class");
        assertUberJarContains(uberJarFile, "org/acme/serviceloader/ServiceProvider.class");
        assertUberJarNotContains(uberJarFile, "org/acme/serviceloader/UnusedProvider.class");
        assertUberJarContains(uberJarFile, "org/acme/forname/ForNameTarget.class");
        assertUberJarNotContains(uberJarFile, "org/acme/forname/UnusedForName.class");
        assertUberJarContains(uberJarFile, "org/acme/transform/TransformAddedRef.class");
        assertUberJarNotContains(uberJarFile, "org/acme/transform/UnusedTransform.class");
        assertUberJarContains(uberJarFile, "org/acme/libreflection/ReflectionTarget.class");
        assertUberJarNotContains(uberJarFile, "org/acme/libreflection/UnusedReflection.class");
        assertUberJarContains(uberJarFile, "org/acme/libjni/JniTarget.class");
        assertUberJarNotContains(uberJarFile, "org/acme/libjni/UnusedJni.class");
        assertUberJarContains(uberJarFile, "org/acme/serialization/SerializedTarget.class");
        assertUberJarNotContains(uberJarFile, "org/acme/serialization/UnusedSerialization.class");
        assertUberJarContains(uberJarFile, "org/acme/loadchain/AlphaTarget.class");
        assertUberJarNotContains(uberJarFile, "org/acme/loadchain/UnusedChain.class");
    }
}
