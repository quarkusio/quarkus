package io.quarkus.deployment.pkg.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.pkg.steps.treeshake.FindClassLoaderUtil;
import io.quarkus.deployment.pkg.steps.treeshake.ForName3ArgUtil;
import io.quarkus.deployment.pkg.steps.treeshake.ForNameInInit;
import io.quarkus.deployment.pkg.steps.treeshake.ForNameModuleUtil;
import io.quarkus.deployment.pkg.steps.treeshake.ForNameProvider;
import io.quarkus.deployment.pkg.steps.treeshake.ForNameUtil;
import io.quarkus.deployment.pkg.steps.treeshake.LoadClassResolveUtil;
import io.quarkus.deployment.pkg.steps.treeshake.LoadClassUtil;
import io.quarkus.deployment.pkg.steps.treeshake.LoadedByMap;
import io.quarkus.deployment.pkg.steps.treeshake.MHFindClassUtil;
import io.quarkus.deployment.pkg.steps.treeshake.MapProvider;
import io.quarkus.deployment.pkg.steps.treeshake.MapTarget;
import io.quarkus.deployment.pkg.steps.treeshake.Target;

class ClassLoadingChainAnalyzerTest {

    /**
     * Verifies that RecordingClassLoader records the name of a successfully loaded class.
     */
    @Test
    void recordingClassLoaderCapturesLoadedClasses() throws Exception {
        ClassLoader loader = createRecordingClassLoader(Target.class);
        loader.loadClass(Target.class.getName());

        Set<String> loaded = getLoadedClassNames(loader);
        assertTrue(loaded.contains(Target.class.getName()), "Should have recorded the loaded class name");
    }

    /**
     * Verifies that RecordingClassLoader records class names even when loading fails.
     */
    @Test
    void recordingClassLoaderCapturesFailedAttempts() throws Exception {
        ClassLoader loader = createRecordingClassLoader();

        try {
            loader.loadClass("com.test.NonExistent");
        } catch (ClassNotFoundException e) {
            // expected
        }

        Set<String> loaded = getLoadedClassNames(loader);
        assertTrue(loaded.contains("com.test.NonExistent"),
                "Should have recorded the class name even though loading failed");
    }

    /**
     * Verifies that when ForNameInInit calls Class.forName(Target) in its constructor,
     * loading and instantiating it causes Target to be recorded too.
     */
    @Test
    void recordingClassLoaderCapturesDependencyChain() throws Exception {
        ClassLoader loader = createRecordingClassLoader(ForNameInInit.class, Target.class);
        Class<?> clazz = Class.forName(ForNameInInit.class.getName(), true, loader);
        clazz.getConstructor().newInstance();

        Set<String> loaded = getLoadedClassNames(loader);
        assertTrue(loaded.contains(ForNameInInit.class.getName()), "Should have recorded ForNameInInit");
        assertTrue(loaded.contains(Target.class.getName()),
                "Should have recorded Target loaded by ForNameInInit's constructor");
    }

    /**
     * Tests the full analyze() flow: ForNameUtil has a method that calls Class.forName(),
     * ForNameProvider's constructor calls ForNameUtil.load(Target). Provider and Util are
     * reachable but Target is not. Verify Target is discovered.
     */
    @Test
    void analyzeFindsClassesLoadedDuringInit() {
        Map<String, Supplier<byte[]>> allBytecode = bytecodeMapOf(
                ForNameUtil.class, ForNameProvider.class, Target.class);
        Set<String> reachable = Set.of(ForNameUtil.class.getName(), ForNameProvider.class.getName());

        Set<String> discovered = analyzeInForkedJvm(reachable, allBytecode);
        assertTrue(discovered.contains(Target.class.getName()),
                "Should discover Target class loaded during ForNameProvider's init");
        assertFalse(discovered.contains(ForNameUtil.class.getName()), "ForNameUtil is already reachable");
        assertFalse(discovered.contains(ForNameProvider.class.getName()), "ForNameProvider is already reachable");
    }

    /**
     * Tests that when an entry point class extends HashMap, stores class names as values,
     * and also has a class-loading chain, the map values that match known classes are discovered.
     */
    @Test
    void analyzeExtractsMapValues() {
        Map<String, Supplier<byte[]>> allBytecode = bytecodeMapOf(
                MapProvider.class, MapTarget.class, LoadedByMap.class);
        Set<String> reachable = Set.of(MapProvider.class.getName());

        Set<String> discovered = analyzeInForkedJvm(reachable, allBytecode);
        assertTrue(discovered.contains(MapTarget.class.getName()),
                "Should discover MapTarget from map values in MapProvider");
    }

    /**
     * Tests that when app bytecode and transformed (dep) bytecode exist for the same class,
     * the transformed version is used. The original bytecode doesn't reference the target,
     * but the transformed version calls Class.forName() for it.
     */
    @Test
    void analyzeUsesTransformedBytecodeOverOriginal() {
        String appClass = "com.test.TransformedApp";

        // Original: simple class, no class loading
        // Transformed: constructor calls Class.forName(Target)
        byte[] originalBytecode = generateSimpleClass(appClass);
        byte[] transformedBytecode = generateClassThatLoads(appClass, Target.class.getName());

        Map<String, Supplier<byte[]>> allBytecode = new HashMap<>();
        allBytecode.put(appClass, () -> originalBytecode); // app version first
        allBytecode.put(appClass, () -> transformedBytecode); // transformed overwrites
        addBytecode(allBytecode, Target.class);

        Set<String> reachable = Set.of(appClass);

        Set<String> discovered = analyzeInForkedJvm(reachable, allBytecode);
        assertTrue(discovered.contains(Target.class.getName()),
                "Should discover Target via transformed bytecode, not original");
    }

    /**
     * Negative case: when only the original (non-transformed) app bytecode is available,
     * the target class is NOT discovered.
     */
    @Test
    void analyzeDoesNotDiscoverTargetFromOriginalBytecode() {
        String appClass = "com.test.OriginalApp";

        byte[] originalBytecode = generateSimpleClass(appClass);

        Map<String, Supplier<byte[]>> allBytecode = new HashMap<>();
        allBytecode.put(appClass, () -> originalBytecode);
        addBytecode(allBytecode, Target.class);

        Set<String> reachable = Set.of(appClass);

        Set<String> discovered = analyzeInForkedJvm(reachable, allBytecode);
        assertFalse(discovered.contains(Target.class.getName()),
                "Should NOT discover Target when only original bytecode is used");
    }

    /**
     * Tests that ClassLoader.loadClass(String) is recognized as a class-loading seed.
     */
    @Test
    void analyzeFindsClassesLoadedViaClassLoaderLoadClass() {
        assertSeedDiscovery(LoadClassUtil.class);
    }

    /**
     * Tests that ClassLoader.loadClass(String, boolean) is recognized as a class-loading seed.
     * Uses Phase 1+2 only since the protected method cannot be executed through the
     * RecordingClassLoader in Phase 3.
     */
    @Test
    void analyzeFindsClassesLoadedViaClassLoaderLoadClassResolve() {
        assertSeedPropagation(LoadClassResolveUtil.class);
    }

    /**
     * Tests that Class.forName(String, boolean, ClassLoader) is recognized as a class-loading seed.
     */
    @Test
    void analyzeFindsClassesLoadedViaClassForName3Arg() {
        assertSeedDiscovery(ForName3ArgUtil.class);
    }

    /**
     * Tests that ClassLoader.findClass(String) is recognized as a class-loading seed.
     * Uses Phase 1+2 only since the protected method cannot be executed through the
     * RecordingClassLoader in Phase 3.
     */
    @Test
    void analyzeFindsClassesLoadedViaClassLoaderFindClass() {
        assertSeedPropagation(FindClassLoaderUtil.class);
    }

    /**
     * Tests that Class.forName(Module, String) is recognized as a class-loading seed.
     * Uses Phase 1+2 only since module-based loading does not route through the
     * RecordingClassLoader in Phase 3.
     */
    @Test
    void analyzeFindsClassesLoadedViaClassForNameModule() {
        assertSeedPropagation(ForNameModuleUtil.class);
    }

    /**
     * Tests that MethodHandles.Lookup.findClass() is recognized as a class-loading seed.
     */
    @Test
    void analyzeFindsClassesLoadedViaMethodHandlesFindClass() {
        assertSeedDiscovery(MHFindClassUtil.class);
    }

    // ---- Seed test helpers ----

    /**
     * Verifies that instantiating the util class in a forked JVM discovers Target
     * through the seed method used in the util's constructor.
     */
    private void assertSeedDiscovery(Class<?> utilClass) {
        Map<String, Supplier<byte[]>> allBytecode = bytecodeMapOf(utilClass, Target.class);
        Set<String> reachable = Set.of(utilClass.getName());

        Set<String> discovered = analyzeInForkedJvm(reachable, allBytecode);
        assertTrue(discovered.contains(Target.class.getName()),
                "Should discover Target loaded via " + utilClass.getSimpleName());
    }

    /**
     * Verifies Phase 1+2 only: the seed call in the util's constructor causes
     * the util class to be identified as an entry point.
     */
    private void assertSeedPropagation(Class<?> utilClass) {
        Map<String, Supplier<byte[]>> allBytecode = bytecodeMapOf(utilClass, Target.class);
        Map<String, Set<String>> callerIndex = buildCallerIndex(allBytecode);
        ClassLoadingChainAnalyzer analyzer = new ClassLoadingChainAnalyzer(callerIndex, allBytecode.keySet());
        Set<String> entryPoints = analyzer.findEntryPoints();
        assertTrue(entryPoints.contains(utilClass.getName()),
                "Should identify " + utilClass.getSimpleName() + " as entry point");
    }

    // ---- Bytecode helpers ----

    /** Reads the compiled bytecode of a class from the test classpath. */
    private static byte[] readBytecode(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = ClassLoadingChainAnalyzerTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Class not found on classpath: " + clazz.getName());
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Builds a class-name-to-bytecode-supplier map from compiled test fixture classes. */
    private static Map<String, Supplier<byte[]>> bytecodeMapOf(Class<?>... classes) {
        Map<String, Supplier<byte[]>> map = new HashMap<>();
        for (Class<?> clazz : classes) {
            addBytecode(map, clazz);
        }
        return map;
    }

    /** Adds a single class's bytecode to the map. */
    private static void addBytecode(Map<String, Supplier<byte[]>> map, Class<?> clazz) {
        byte[] bytecode = readBytecode(clazz);
        map.put(clazz.getName(), () -> bytecode);
    }

    /**
     * Builds a reverse caller index from bytecode, using
     * {@link ClassLoadingChainAnalyzer#shouldRecordCall} for filtering.
     */
    private static Map<String, Set<String>> buildCallerIndex(Map<String, Supplier<byte[]>> allBytecode) {
        Map<String, Set<String>> callerIndex = new HashMap<>();
        for (var entry : allBytecode.entrySet()) {
            String internalOwner = entry.getKey().replace('.', '/');
            ClassReader reader = new ClassReader(entry.getValue().get());
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    String callerKey = internalOwner + "." + name + descriptor;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mname,
                                String mdescriptor, boolean isInterface) {
                            String calleeKey = owner + "." + mname + mdescriptor;
                            if (ClassLoadingChainAnalyzer.shouldRecordCall(calleeKey)) {
                                callerIndex.computeIfAbsent(calleeKey, k -> new HashSet<>()).add(callerKey);
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        }
        return callerIndex;
    }

    /**
     * Runs the full analysis (Phases 1+2 in-process, Phase 3 in a forked JVM)
     * and returns newly discovered class names.
     */
    private static Set<String> analyzeInForkedJvm(Set<String> reachable,
            Map<String, Supplier<byte[]>> allBytecode) {
        Set<String> allKnown = allBytecode.keySet();
        Set<String> discovered = ClassLoadingChainAnalyzer.executeEntryPoints(
                reachable, allBytecode, Map.of(),
                allKnown, List.of(), List.of());
        discovered.retainAll(allKnown);
        discovered.removeAll(reachable);
        return discovered;
    }

    // ---- ASM generators (only used for transformed bytecode tests) ----

    /** Generates a simple class with a no-arg constructor that does nothing. */
    private static byte[] generateSimpleClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String internalName = className.replace('.', '/');
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Generates a class whose constructor calls Class.forName(targetClassName). */
    private static byte[] generateClassThatLoads(String className, String targetClassName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String internalName = className.replace('.', '/');
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitLdcInsn(targetClassName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---- Reflection helpers to access RecordingClassLoader ----

    private static ClassLoader createRecordingClassLoader(Class<?>... classes) throws Exception {
        Path tempDir = Files.createTempDirectory("test-recording");
        for (Class<?> clazz : classes) {
            byte[] bytecode = readBytecode(clazz);
            String classFile = clazz.getName().replace('.', '/') + ".class";
            Path target = tempDir.resolve(classFile);
            Files.createDirectories(target.getParent());
            Files.write(target, bytecode);
        }

        Class<?> recordingClass = Class.forName(ClassLoadingRecorder.class.getName() + "$RecordingClassLoader");
        Constructor<?> ctor = recordingClass.getDeclaredConstructor(URL[].class);
        ctor.setAccessible(true);
        return (ClassLoader) ctor.newInstance(
                (Object) new URL[] { tempDir.toUri().toURL() });
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getLoadedClassNames(ClassLoader loader) throws Exception {
        java.lang.reflect.Method method = loader.getClass().getDeclaredMethod("getLoadedClassNames");
        method.setAccessible(true);
        return (Set<String>) method.invoke(loader);
    }
}
