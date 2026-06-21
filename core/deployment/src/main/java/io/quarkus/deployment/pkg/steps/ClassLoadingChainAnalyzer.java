package io.quarkus.deployment.pkg.steps;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
 * managed by {@link ForkedJvmEnvironment} (which launches {@link ClassLoadingRecorder})
 * to dynamically capture all class load attempts that occur during its initialization
 * and construction. Running in a separate process ensures complete isolation of global
 * JVM state and that Metaspace is reclaimed when the process exits.
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

    private final Set<String> depClassNames;

    /**
     * Reverse call index: callee method → set of caller methods.
     * Built externally by {@link JarTreeShaker#scanBytecode} during BFS reachability
     * analysis. For each method invocation encountered in bytecode, the invoked method
     * (callee) is mapped to the set of methods that invoke it (callers). This enables
     * backward propagation from class-loading seed methods to discover all application
     * methods that transitively trigger dynamic class loading.
     */
    private final Map<MethodKey, Set<MethodKey>> callerIndex;

    private final Set<MethodKey> classLoadingMethods = new HashSet<>(MethodKey.SEED_METHOD_KEYS);

    /** Entry points already returned by previous {@link #findEntryPoints()} calls. */
    private final Set<String> previousEntryPoints = new HashSet<>();
    private boolean seedsPropagated = false;

    /**
     * @param callerIndex pre-built reverse call index (callee → callers), populated during BFS
     * @param depClassNames dependency class names (dot-separated) for entry point filtering
     */
    ClassLoadingChainAnalyzer(Map<MethodKey, Set<MethodKey>> callerIndex, Set<String> depClassNames) {
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
        Set<MethodKey> newMethods = propagateFromSeeds();
        if (newMethods.isEmpty()) {
            return Set.of();
        }
        return extractNewEntryPoints(newMethods);
    }

    /**
     * Propagates from seed methods through the caller index using a worklist algorithm.
     * On first call, walks from all seeds. On subsequent calls, checks new edges added
     * by a prior BFS iteration.
     *
     * @return methods newly added to {@link #classLoadingMethods} in this invocation
     */
    private Set<MethodKey> propagateFromSeeds() {
        Set<MethodKey> newMethods = new HashSet<>();
        Queue<MethodKey> callerQueue = new ArrayDeque<>();
        if (!seedsPropagated) {
            seedsPropagated = true;
            for (MethodKey seed : MethodKey.SEED_METHOD_KEYS) {
                Set<MethodKey> callers = callerIndex.get(seed);
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
            MethodKey method = callerQueue.poll();
            Set<MethodKey> callers = callerIndex.get(method);
            if (callers != null) {
                collectAndEnqueueCallers(callers, callerQueue, newMethods);
            }
        }
        return newMethods;
    }

    /**
     * Adds non-JDK/infra callers to the worklist queue and marks them as class-loading methods.
     */
    private void collectAndEnqueueCallers(Set<MethodKey> callers, Queue<MethodKey> callerQueue,
            Set<MethodKey> newMethods) {
        for (MethodKey caller : callers) {
            if (!MethodKey.isJdkOrInfraClass(caller.owner) && classLoadingMethods.add(caller)) {
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
    private Set<String> extractNewEntryPoints(Set<MethodKey> newMethods) {
        Set<String> entryPointClasses = null;
        for (MethodKey method : newMethods) {
            String entryPoint = getEntryPointOrNull(method);
            if (entryPoint != null && previousEntryPoints.add(entryPoint)) {
                if (entryPointClasses == null) {
                    entryPointClasses = new HashSet<>();
                }
                entryPointClasses.add(entryPoint);
            }
        }
        return entryPointClasses == null ? Set.of() : entryPointClasses;
    }

    /**
     * Returns the dot-separated class name if the method key is an {@code <init>} or
     * {@code <clinit>} of a known dependency class, or {@code null} otherwise.
     * Uses {@link MethodKey#name} directly instead of parsing a concatenated string.
     */
    private String getEntryPointOrNull(MethodKey key) {
        if (key.isInitOrClinit()) {
            String dotName = key.ownerAsDotName();
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
}
