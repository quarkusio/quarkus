package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * Discovers classes that are loaded dynamically via {@code ClassLoader.loadClass()} or
 * {@code Class.forName()} through call chains that cannot be statically traced by the
 * tree-shaker's BFS reachability analysis.
 *
 * <h2>Problem</h2>
 * <p>
 * Many libraries construct class names at runtime using {@code StringBuilder},
 * {@code invokedynamic} {@code StringConcatFactory}, or similar mechanisms, and then load
 * them via {@code ClassLoader.loadClass()} or {@code Class.forName()} during static
 * initialization ({@code <clinit>}) or constructor execution ({@code <init>}). Because
 * the class name strings are computed dynamically, the tree-shaker's BFS over constant
 * pool references and method instructions cannot trace which classes will actually be
 * loaded. Without special handling, these dynamically loaded classes would be incorrectly
 * removed from the final artifact.
 *
 * <h2>Solution overview</h2>
 * <p>
 * Rather than attempting to reconstruct class name strings from bytecode (which is
 * fragile and incomplete), this analyzer uses a three-phase approach:
 *
 * <h3>Phase 1 -- Seed propagation</h3>
 * <p>
 * Uses a reverse caller index (callee → callers) that is built externally by
 * {@link JarTreeShaker#scanBytecode} during BFS reachability analysis. Starting from seed
 * methods ({@code ClassLoader.loadClass(String)}, {@code Class.forName(String)},
 * {@code Class.forName(String, boolean, ClassLoader)}, and
 * {@code MethodHandles.Lookup.findClass(String)}), propagates backwards through the
 * caller index using a fixed-point algorithm to identify every application method that
 * transitively calls a class-loading seed. JDK and infrastructure classes
 * ({@code java/}, {@code javax/}, {@code jakarta/}, {@code sun/}, {@code org/objectweb/})
 * are excluded from propagation to avoid false positives.
 *
 * <h3>Phase 2 -- Entry point discovery</h3>
 * <p>
 * Scans the class-loading methods identified in Phase 1 for {@code <init>} or
 * {@code <clinit>} methods. These represent "entry point" classes whose construction
 * or static initialization ultimately triggers the dynamic class loading. No additional
 * graph walk is needed because Phase 1 already computed the full transitive closure.
 *
 * <h3>Phase 3 -- Dynamic execution in a forked JVM</h3>
 * <p>
 * Each entry point class is loaded and instantiated inside a forked JVM process
 * (see {@link ClassLoadingRecorder}) to dynamically capture all class load attempts that
 * occur during its initialization and construction. Running in a separate process
 * ensures complete isolation of global JVM state and that Metaspace is reclaimed
 * when the process exits.
 *
 * <h2>RecordingClassLoader</h2>
 * <p>
 * The {@link ClassLoadingRecorder.RecordingClassLoader} extends {@code URLClassLoader} with
 * the platform class loader as its parent, isolating it from the application classpath.
 * It loads dependency and application classes from URLs built from the forked JVM's
 * classpath. Every {@code loadClass()} attempt is recorded, including attempts that fail
 * with {@code ClassNotFoundException}. After instantiation, if the resulting object
 * implements {@code java.util.Map}, the analyzer also extracts {@code String} values from
 * the map entries and checks them against known class names. This handles patterns like
 * BouncyCastle's {@code addAlgorithm()} which stores class names as map values for
 * deferred loading rather than loading them immediately.
 *
 * <h2>Fixed-point loop</h2>
 * <p>
 * This analyzer is designed to be called from {@code JarTreeShaker} in an outer
 * fixed-point loop. Each invocation returns newly discovered class names. These are fed
 * back into the BFS reachability analysis, which may mark additional classes as
 * reachable, and the analysis repeats. The loop terminates when no new classes are
 * discovered in an iteration.
 *
 * <h2>Example: BouncyCastle</h2>
 * <p>
 * BouncyCastle's {@code BouncyCastleProvider} constructor calls {@code setup()}, which
 * calls {@code loadAlgorithms()}, which calls {@code loadServiceClass()}, which calls
 * {@code ClassUtil.loadClass()}, which ultimately calls {@code ClassLoader.loadClass()}.
 * Phase 1 marks all these methods as class-loading methods. Phase 2 walks up to the
 * {@code BouncyCastleProvider.<init>} constructor and identifies it as an entry point.
 * Phase 3 instantiates it and records every dynamically loaded class. Additionally, each
 * {@code $Mappings} class stores sub-class names via {@code addAlgorithm(key, className)},
 * which populates the provider's internal map. These class names are captured by
 * inspecting the map values after instantiation.
 */
class ClassLoadingChainAnalyzer {

    private static final Logger log = Logger.getLogger(ClassLoadingChainAnalyzer.class.getName());

    /** JDK methods that load classes by name — the seeds for call chain propagation. */
    static final Set<String> SEED_METHODS = Set.of(
            "java/lang/ClassLoader.loadClass(Ljava/lang/String;)Ljava/lang/Class;",
            "java/lang/ClassLoader.loadClass(Ljava/lang/String;Z)Ljava/lang/Class;",
            "java/lang/ClassLoader.findClass(Ljava/lang/String;)Ljava/lang/Class;",
            "java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;",
            "java/lang/Class.forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
            "java/lang/Class.forName(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;",
            "java/lang/invoke/MethodHandles$Lookup.findClass(Ljava/lang/String;)Ljava/lang/Class;");

    private final Set<String> depClassNames;
    /** Reverse call index: callee method → set of caller methods. Built externally during BFS. */
    private final Map<String, Set<String>> callerIndex;
    private final Set<String> classLoadingMethods = new HashSet<>(SEED_METHODS);
    /** Entry points already returned by previous {@link #findEntryPoints()} calls. */
    private final Set<String> previousEntryPoints = new HashSet<>();
    private boolean seedsPropagated = false;

    /**
     * @param callerIndex pre-built reverse call index (callee → callers), populated during BFS
     * @param depClassNames dependency class names (dot-separated) for entry point filtering
     */
    ClassLoadingChainAnalyzer(Map<String, Set<String>> callerIndex, Set<String> depClassNames) {
        this.callerIndex = callerIndex;
        this.depClassNames = depClassNames;
    }

    /**
     * Propagates backwards from class-loading seed methods through the pre-built
     * caller index to find entry point classes whose {@code <init>}/{@code <clinit>}
     * triggers dynamic class loading. Only returns entry points not returned by
     * previous invocations.
     *
     * @return newly discovered entry point class names, or empty set if none found
     */
    Set<String> findEntryPoints() {
        Set<String> newMethods = propagateFromSeeds();
        if (newMethods.isEmpty()) {
            return Set.of();
        }
        return extractNewEntryPoints(newMethods);
    }

    /**
     * Propagates from seed methods through the caller index using a worklist algorithm.
     * On first call, walks from all seeds. On subsequent calls, checks new edges.
     *
     * @return methods newly added to {@link #classLoadingMethods} in this invocation
     */
    private Set<String> propagateFromSeeds() {
        Set<String> newMethods = new HashSet<>();
        Queue<String> callerQueue = new ArrayDeque<>();
        if (!seedsPropagated) {
            seedsPropagated = true;
            for (String seed : SEED_METHODS) {
                Set<String> callers = callerIndex.get(seed);
                if (callers != null) {
                    collectAndEnqueueCallers(callers, callerQueue, newMethods);
                }
            }
        } else {
            for (var entry : callerIndex.entrySet()) {
                if (classLoadingMethods.contains(entry.getKey())) {
                    collectAndEnqueueCallers(entry.getValue(), callerQueue, newMethods);
                }
            }
        }
        while (!callerQueue.isEmpty()) {
            String method = callerQueue.poll();
            Set<String> callers = callerIndex.get(method);
            if (callers != null) {
                collectAndEnqueueCallers(callers, callerQueue, newMethods);
            }
        }
        return newMethods;
    }

    private void collectAndEnqueueCallers(Set<String> callers, Queue<String> callerQueue, Set<String> newMethods) {
        for (String caller : callers) {
            if (!isJdkOrInfraClass(caller) && classLoadingMethods.add(caller)) {
                callerQueue.add(caller);
                newMethods.add(caller);
            }
        }
    }

    /**
     * Scans newly discovered class-loading methods for {@code <init>} or {@code <clinit>}
     * entry points not found by previous invocations. No additional graph walk is needed
     * because {@link #propagateFromSeeds()} already computed the full transitive closure
     * of callers.
     */
    private Set<String> extractNewEntryPoints(Set<String> newMethods) {
        Set<String> entryPointClasses = new HashSet<>();
        for (String method : newMethods) {
            String entryPoint = getEntryPointOrNull(method);
            if (entryPoint != null && previousEntryPoints.add(entryPoint)) {
                entryPointClasses.add(entryPoint);
            }
        }
        return entryPointClasses;
    }

    /**
     * Returns the dot-separated class name if the method key is an {@code <init>} or
     * {@code <clinit>} of a known dependency class, or {@code null} otherwise.
     */
    private String getEntryPointOrNull(String methodKey) {
        int parenIdx = methodKey.indexOf('(');
        if (parenIdx < 0) {
            return null;
        }
        int dotIdx = methodKey.lastIndexOf('.', parenIdx);
        if (dotIdx < 0) {
            return null;
        }
        String methodName = methodKey.substring(dotIdx + 1, parenIdx);
        if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
            String dotName = methodKey.substring(0, dotIdx).replace('/', '.');
            if (depClassNames.contains(dotName)) {
                return dotName;
            }
        }
        return null;
    }

    /**
     * Releases the class-loading method data to free memory.
     * The caller index is owned externally and cleared by the caller.
     */
    void release() {
        classLoadingMethods.clear();
        previousEntryPoints.clear();
    }

    /**
     * Phase 3: Execute entry point classes in a forked JVM to capture which classes
     * get loaded during static initialization and construction.
     * <p>
     * Running in a separate process ensures complete isolation of global JVM state
     * (System.out/err, security providers, system properties, TCCL) and that all
     * Metaspace consumed by {@code defineClass} is reclaimed when the process exits.
     * This is critical because multiple tree-shaker instances may run in parallel
     * during CI builds with {@code -T2C}.
     * <p>
     * Only generated and transformed bytecode is written to a temp directory.
     * Dependency JARs and the app artifact are included on the classpath by path,
     * with the temp dir first so transformed classes override originals.
     *
     * @param entryPointClasses classes whose init/clinit triggers dynamic class loading
     * @param generatedBytecode generated classes (in-memory only, must be written to disk)
     * @param transformedBytecode transformed classes that override originals in dep JARs
     * @param allKnownClasses all known class names for Map value extraction
     * @param depJarPaths file system paths to dependency JARs
     * @param appPaths file system paths to the application artifact
     */
    static Set<String> executeEntryPoints(
            Set<String> entryPointClasses,
            Map<String, Supplier<byte[]>> generatedBytecode,
            Map<String, Supplier<byte[]>> transformedBytecode,
            Set<String> allKnownClasses,
            List<Path> depJarPaths,
            List<Path> appPaths) {

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tree-shake-recording");

            // Write only generated and transformed bytecode to temp dir
            writeBytecodeToDir(tempDir, generatedBytecode);
            writeBytecodeToDir(tempDir, transformedBytecode);

            // Write input file: entry points, then separator, then allKnownClasses
            Path inputFile = tempDir.resolve("_input.txt");
            List<String> inputLines = new ArrayList<>(entryPointClasses.size() + allKnownClasses.size() + 1);
            inputLines.addAll(entryPointClasses);
            inputLines.add("---");
            inputLines.addAll(allKnownClasses);
            Files.write(inputFile, inputLines, StandardCharsets.UTF_8);

            // Copy ClassLoadingRecorder class to the temp dir
            writeRecorderClass(tempDir);

            // Build classpath: temp dir first (overrides), then dep JARs, then app
            StringBuilder cpBuilder = new StringBuilder();
            cpBuilder.append(tempDir);
            for (Path p : depJarPaths) {
                cpBuilder.append(File.pathSeparator).append(p);
            }
            for (Path p : appPaths) {
                cpBuilder.append(File.pathSeparator).append(p);
            }

            // Write @argfile to avoid command-line length limits (Windows)
            Path argFile = tempDir.resolve("_jvm.args");
            Files.writeString(argFile,
                    "-cp\n" + cpBuilder + "\n-XX:MaxMetaspaceSize=256m",
                    StandardCharsets.UTF_8);

            // Fork JVM
            String javaCmd = ProcessHandle.current().info().command()
                    .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());

            ProcessBuilder pb = new ProcessBuilder(
                    javaCmd,
                    "@" + argFile,
                    ClassLoadingRecorder.class.getName(),
                    inputFile.toString());
            pb.redirectErrorStream(false);

            Process proc = pb.start();

            // Drain stderr in background to avoid blocking
            Thread stderrDrainer = new Thread(() -> {
                try {
                    proc.getErrorStream().transferTo(OutputStream.nullOutputStream());
                } catch (IOException ignored) {
                }
            }, "tree-shake-stderr-drainer");
            stderrDrainer.setDaemon(true);
            stderrDrainer.start();

            // Read discovered class names from stdout
            Set<String> discovered = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        discovered.add(line);
                    }
                }
            }

            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                log.warnf("Class-loading chain analysis forked JVM exited with code %d", exitCode);
            }
            stderrDrainer.join(5000);

            return discovered;

        } catch (IOException | InterruptedException e) {
            log.warnf(e, "Failed to run class-loading chain analysis in forked JVM, skipping Phase 3");
            return Set.of();
        } finally {
            if (tempDir != null) {
                deleteRecursive(tempDir);
            }
        }
    }

    private static void writeBytecodeToDir(Path dir, Map<String, Supplier<byte[]>> bytecodeMap) throws IOException {
        for (var entry : bytecodeMap.entrySet()) {
            byte[] bytes;
            try {
                bytes = entry.getValue().get();
            } catch (Exception e) {
                continue;
            }
            String classFile = entry.getKey().replace('.', '/') + ".class";
            Path target = dir.resolve(classFile);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        }
    }

    /**
     * Writes the {@link ClassLoadingRecorder} class files to the temp directory.
     */
    private static void writeRecorderClass(Path tempDir) throws IOException {
        String baseName = ClassLoadingRecorder.class.getName().replace('.', '/');
        for (String suffix : new String[] { ".class", "$RecordingClassLoader.class" }) {
            String resourcePath = baseName + suffix;
            try (var is = ClassLoadingChainAnalyzer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    if (suffix.equals(".class")) {
                        throw new IOException("Cannot find ClassLoadingRecorder.class on classpath");
                    }
                    continue;
                }
                Path target = tempDir.resolve(resourcePath);
                Files.createDirectories(target.getParent());
                Files.write(target, is.readAllBytes());
            }
        }
    }

    private static void deleteRecursive(Path path) {
        try {
            Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Checks whether a method key belongs to a JDK or infrastructure class that should not
     * be considered as a class-loading method for propagation purposes.
     */
    static boolean isJdkOrInfraClass(String methodKey) {
        return methodKey.startsWith("java/")
                || methodKey.startsWith("javax/")
                || methodKey.startsWith("jakarta/")
                || methodKey.startsWith("org/objectweb/")
                || methodKey.startsWith("sun/");
    }

    /**
     * Returns {@code true} if a method call to {@code calleeKey} should be recorded
     * in the caller index. Records calls to seed methods and calls to non-JDK/infra methods.
     */
    static boolean shouldRecordCall(String calleeKey) {
        return SEED_METHODS.contains(calleeKey) || !isJdkOrInfraClass(calleeKey);
    }
}
