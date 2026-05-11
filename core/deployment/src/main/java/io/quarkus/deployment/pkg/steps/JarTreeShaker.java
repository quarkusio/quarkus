package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.OpenPathTree;

class JarTreeShaker {

    private static final Logger log = Logger.getLogger(JarTreeShaker.class.getName());

    private static final String SERVICE_LOADER_INTERNAL = "java/util/ServiceLoader";
    private static final String SISU_NAMED_RESOURCE = "META-INF/sisu/javax.inject.Named";

    /**
     * Suffixes for JBoss Logging companion classes that are loaded reflectively
     * via name concatenation in {@code Logger.getMessageLogger()}.
     */
    private static final String[] JBOSS_LOGGING_SUFFIXES = { "_$logger", "_$bundle", "_impl" };

    private final JarTreeShakeInput input;

    /**
     * Reverse call graph built during BFS: maps each callee method to the set of methods
     * that call it. Used by {@link ClassLoadingChainAnalyzer} to discover classes that
     * dynamically load other classes at runtime.
     *
     * <p>
     * <b>Seeds</b> are JDK methods that load classes by name:
     * {@code ClassLoader.loadClass()}, {@code Class.forName()}, and
     * {@code MethodHandles.Lookup.findClass()}. The analyzer propagates backwards
     * from these seeds through the caller index to find every application method
     * that transitively invokes dynamic class loading.
     *
     * <p>
     * <b>Entry points</b> are {@code <init>} or {@code <clinit>} methods found
     * along these call chains — classes whose construction or static initialization
     * ultimately triggers the dynamic loading. These entry points are then executed
     * in an isolated classloader to capture which classes are actually loaded at runtime.
     */
    private final Map<MethodKey, Set<MethodKey>> callerIndex = new HashMap<>();

    JarTreeShaker(JarTreeShakeInput input) {
        this.input = input;
    }

    /**
     * Runs the tree-shake analysis: traces reachable classes from roots via BFS,
     * evaluates conditional roots to a fixed point, computes removal stats,
     * and returns a {@link JarTreeShakeBuildItem} with the reachable set and per-dependency removals.
     */
    JarTreeShakeBuildItem run() {
        final long startTime = System.currentTimeMillis();
        final boolean debug = log.isDebugEnabled();

        // Trace all reachable classes using bytecode analysis for full method-body coverage
        Set<String> visited = new HashSet<>();
        Set<String> reachable = traceReachableClasses(input.roots, visited);
        if (debug) {
            log.debugf("BFS reachability: %d classes reached from %d roots, %d caller index entries",
                    reachable.size(), input.roots.size(), callerIndex.size());
        }

        // Evaluate conditional roots to fixed point
        reachable = evaluateConditionalRoots(reachable);

        // Analyze class-loading chains (fixed-point loop) — skip if no seed methods
        // (ClassLoader.loadClass, Class.forName, etc.) were called by any reachable code
        if (hasCallerIndexSeedEntries()) {
            reachable = analyzeClassLoadingChains(reachable);
        } else {
            if (debug) {
                log.debug("Class-loading chain analysis skipped: no seed method calls found");
            }
            callerIndex.clear();
        }

        // Mark all classes from excluded artifacts as reachable (after analysis,
        // so they don't act as roots for transitive tracing). This preserves
        // JARs that perform self-integrity checks (e.g. BouncyCastle FIPS).
        if (!input.excludedArtifacts.isEmpty()) {
            for (var entry : input.classToDep.entrySet()) {
                if (input.excludedArtifacts.contains(entry.getValue())) {
                    reachable.add(entry.getKey());
                }
            }
        }

        // Trace references from higher-version multi-release bytecode of reachable classes.
        // These variants may reference classes not present in the analyzed version
        // (e.g. BouncyCastle's version-25 SSLSessionUtil references ImportSSLSession_25).
        if (!input.higherVersionBytecode.isEmpty()) {
            traceHigherVersionReferences(reachable);
        }

        // Compute removal stats before releasing analysis data
        int totalDepClasses = input.depBytecode.size();
        int removedClassCount = 0;
        long removedBytes = 0;
        final Map<ArtifactKey, List<String>> removedClassesPerDep = new HashMap<>();
        for (var entry : input.depBytecode.entrySet()) {
            if (!reachable.contains(entry.getKey())) {
                removedClassCount++;
                removedBytes += bytecodeSize(entry.getValue());
                ArtifactKey dep = input.classToDep.get(entry.getKey());
                if (dep != null) {
                    removedClassesPerDep.computeIfAbsent(dep, k -> new ArrayList<>())
                            .add(entry.getKey().replace('.', '/') + ".class");
                }
            }
        }
        // Sort each list for stable pedigree output
        for (List<String> list : removedClassesPerDep.values()) {
            Collections.sort(list);
        }

        // Release all analysis data structures
        input.releaseAnalysisData();

        // Report
        long elapsedMs = System.currentTimeMillis() - startTime;
        log.infof("Tree-shaking removed %d unreachable classes from %d dependencies,"
                + " saving %s (%.1f%%) of bytecode in %dms",
                removedClassCount,
                removedClassesPerDep.size(),
                formatSize(removedBytes),
                totalDepClasses > 0 ? (removedClassCount * 100.0 / totalDepClasses) : 0.0,
                elapsedMs);

        if (log.isDebugEnabled()) {
            int reachableClasses = totalDepClasses - removedClassCount;
            log.debug("============================================================");
            log.debug("  Dependency Classes Usage Analysis");
            log.debug("============================================================");
            log.debugf("  Total classes    : %d", totalDepClasses);
            log.debugf("  Reachable classes: %d", reachableClasses);
            log.debug("------------------------------------------------------------");
            log.debug("  DEPENDENCIES WITH REMOVED CLASSES:");
            removedClassesPerDep.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ArtifactKey::toGacString)))
                    .forEach(e -> log.debugf("    - %s  (%d classes removed)",
                            e.getKey().toGacString(), e.getValue().size()));
            log.debug("------------------------------------------------------------");
            log.debugf("  Done in %dms", System.currentTimeMillis() - startTime);
            log.debug("============================================================");
        }

        return new JarTreeShakeBuildItem(input.treeShakeMode == TreeShakeConfig.TreeShakeMode.CLASSES, reachable,
                removedClassesPerDep);
    }

    /**
     * Returns {@code true} if the caller index contains at least one entry for a
     * class-loading seed method. When no seeds are present, the entire
     * {@link ClassLoadingChainAnalyzer} phase produces no results and can be skipped.
     */
    private boolean hasCallerIndexSeedEntries() {
        for (MethodKey seed : MethodKey.SEED_METHOD_KEYS) {
            if (callerIndex.containsKey(seed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs class-loading chain analysis in a fixed-point loop, discovering classes
     * that are loaded dynamically via ClassLoader.loadClass() or Class.forName()
     * during static initialization or construction of reachable classes.
     */
    private Set<String> analyzeClassLoadingChains(Set<String> reachable) {
        final boolean debug = log.isDebugEnabled();

        // Build the transformed bytecode map once (optimization B)
        Map<String, Supplier<byte[]>> transformedBytecode = new HashMap<>();
        for (String name : input.transformedClassNames) {
            Supplier<byte[]> supplier = input.depBytecode.get(name);
            if (supplier != null) {
                transformedBytecode.put(name, supplier);
            }
        }

        ClassLoadingChainAnalyzer analyzer = new ClassLoadingChainAnalyzer(callerIndex, input.classToDep.keySet());
        Set<String> allPhase3Discovered = new HashSet<>();
        int iteration = 0;

        try (var env = ForkedJvmEnvironment.create(
                input.generatedBytecode, transformedBytecode,
                input.depJarPaths, input.appPaths)) {

            while (true) {
                iteration++;
                Set<String> newEntryPoints = analyzer.findEntryPoints();
                if (newEntryPoints.isEmpty()) {
                    if (debug) {
                        log.debugf("Class-loading chain iteration %d: no new entry points, stopping", iteration);
                    }
                    break;
                }

                // Skip fork if all new entry points were already discovered by prior forks —
                // their transitive class loads were already captured
                if (allPhase3Discovered.containsAll(newEntryPoints)) {
                    if (debug) {
                        log.debugf("Class-loading chain iteration %d: %d entry points already covered, stopping",
                                iteration, newEntryPoints.size());
                    }
                    break;
                }

                // Phase 3: execute only new entry points in a forked JVM
                Set<String> discovered = env.executeEntryPoints(newEntryPoints, input.allKnownClasses);

                allPhase3Discovered.addAll(discovered);

                // Filter to non-reachable classes
                discovered.removeAll(reachable);
                if (debug) {
                    log.debugf("Class-loading chain iteration %d: %d entry points, forked JVM discovered %d classes"
                            + " (%d new)",
                            iteration, newEntryPoints.size(), allPhase3Discovered.size(),
                            discovered.size());
                }
                if (discovered.isEmpty()) {
                    break;
                }
                traceReachableClasses(discovered, reachable);
                evaluateConditionalRoots(reachable);
            }
        } catch (IOException | InterruptedException e) {
            log.warnf(e, "Failed to run class-loading chain analysis in forked JVM, skipping Phase 3");
        }
        if (debug) {
            log.debugf("Class-loading chain analysis completed: %d iterations, %d total discovered classes",
                    iteration, allPhase3Discovered.size());
        }
        analyzer.release();
        callerIndex.clear();
        return reachable;
    }

    // ---- Reachability tracing ----

    /**
     * BFS traversal from {@code startingRoots}, following bytecode references, service provider
     * relationships, and string-constant class names. Populates and returns the {@code visited} set.
     * Can be called multiple times with the same {@code visited} set to resume from new roots.
     * Also builds the {@link #callerIndex} as a side effect during bytecode scanning.
     */
    private Set<String> traceReachableClasses(Set<String> startingRoots, Set<String> visited) {
        final Queue<String> queue = new ArrayDeque<>();
        for (String root : startingRoots) {
            if (visited.add(root)) {
                queue.add(root);
            }
        }
        boolean sisuActivated = false;
        final Map<ArtifactKey, Integer> depDeserializationFlags = new HashMap<>();
        final BfsScanResult scan = new BfsScanResult();

        while (!queue.isEmpty()) {
            String name = queue.poll();

            enqueueJBossLoggingCompanions(name, visited, queue);
            enqueueServiceProviders(name, visited, queue);

            byte[] bytecode = input.readBytecode(name);
            if (bytecode == null) {
                continue; // JDK class or not available
            }

            // Single-pass ASM scan: extracts all class references, string class refs,
            // ServiceLoader calls, sisu detection, and resource/OIS detection in one pass
            scan.clear();
            scanBytecode(name, bytecode, sisuActivated, scan);

            // Enqueue discovered class references — only those we have bytecode for.
            // Skips JDK/platform classes that we'll never find, avoiding thousands of
            // no-op queue iterations and wasted lookups.
            for (String ref : scan.refs) {
                if (ref != null && input.hasBytecode(ref) && visited.add(ref)) {
                    queue.add(ref);
                }
            }

            // Process ServiceLoader calls
            if (!scan.serviceLoaderServices.isEmpty()) {
                input.serviceLoaderCalls.put(name, scan.serviceLoaderServices);
                for (String serviceInterface : scan.serviceLoaderServices) {
                    Set<String> providers = input.serviceProviders.get(serviceInterface);
                    if (providers != null) {
                        for (String provider : providers) {
                            if (visited.add(provider)) {
                                queue.add(provider);
                            }
                        }
                    }
                }
            }

            // Process sisu detection
            if (!sisuActivated && scan.sisuDetected) {
                sisuActivated = true;
                enqueueNotVisited(input.sisuNamedClasses, visited, queue);
            }

            // Process deserialization detection
            ArtifactKey depKey = input.classToDep.get(name);
            if (depKey != null) {
                int flags = depDeserializationFlags.getOrDefault(depKey, 0);
                if ((flags & FLAG_SCANNED) == 0) {
                    flags |= scan.deserializationFlags;
                    depDeserializationFlags.put(depKey, flags);
                    if ((flags & FLAG_RESOURCE_DESERIALIZATION) == FLAG_RESOURCE_DESERIALIZATION) {
                        depDeserializationFlags.put(depKey, flags | FLAG_SCANNED);
                        OpenPathTree openTree = input.openTrees.get(depKey);
                        if (openTree != null) {
                            Set<String> classNames = extractClassNamesFromSerializedResources(openTree);
                            if (!classNames.isEmpty()) {
                                log.debugf("ObjectInputStream + resource access detected in %s, "
                                        + "found %d serialized classes in %s",
                                        name, classNames.size(), depKey);
                                for (String className : classNames) {
                                    if (visited.add(className)) {
                                        queue.add(className);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return visited;
    }

    /**
     * Iterates conditional roots (from {@link io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem})
     * to a fixed point: when a condition type becomes reachable, its associated class names are added as new roots
     * and traced. Repeats until no new classes are discovered.
     */
    private Set<String> evaluateConditionalRoots(Set<String> reachable) {
        if (input.conditionalRoots.isEmpty()) {
            return reachable;
        }
        Map<String, Set<String>> remaining = new HashMap<>(input.conditionalRoots);
        boolean changed = true;
        int iteration = 0;
        while (changed) {
            changed = false;
            iteration++;
            Set<String> newRoots = new HashSet<>();
            var it = remaining.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                if (reachable.contains(entry.getKey())) {
                    newRoots.addAll(entry.getValue());
                    it.remove();
                }
            }
            newRoots.removeAll(reachable);
            if (!newRoots.isEmpty()) {
                changed = true;
                if (log.isDebugEnabled()) {
                    log.debugf("Conditional roots iteration %d: %d new roots, %d conditions remaining",
                            iteration, newRoots.size(), remaining.size());
                }
                traceReachableClasses(newRoots, reachable);
            }
        }
        return reachable;
    }

    /**
     * For each reachable class that has multi-release bytecode targeting newer Java versions,
     * extracts class references from that bytecode and marks them reachable. This ensures
     * classes referenced only by higher-version variants are preserved (e.g. BouncyCastle's
     * version-25 SSLSessionUtil references ImportSSLSession_25 which doesn't exist in
     * the analyzed version-9 bytecode). Iterates to a fixed point.
     */
    private void traceHigherVersionReferences(Set<String> reachable) {
        // Process reachable entries, removing them as we go to avoid re-scanning.
        // When new classes become reachable, they may have higher-version entries
        // that need processing too, so we loop until no new classes are discovered.
        int preSize = reachable.size();
        boolean changed = true;
        int iteration = 0;
        while (changed) {
            changed = false;
            iteration++;
            var it = input.higherVersionBytecode.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                if (!reachable.contains(entry.getKey())) {
                    continue;
                }
                it.remove();
                for (Supplier<byte[]> supplier : entry.getValue()) {
                    Set<String> refs = extractReferencesFromBytecode(supplier.get());
                    for (String ref : refs) {
                        if (ref != null && input.allKnownClasses.contains(ref) && reachable.add(ref)) {
                            changed = true;
                        }
                    }
                }
            }
        }
        if (log.isDebugEnabled() && reachable.size() > preSize) {
            log.debugf("Multi-release bytecode: %d new classes discovered in %d iterations",
                    reachable.size() - preSize, iteration);
        }
    }

    /**
     * JBoss Logging companion classes (_$logger, _$bundle, _impl) are loaded reflectively
     * via name concatenation in Logger.getMessageLogger().
     */
    private void enqueueJBossLoggingCompanions(String name, Set<String> visited, Queue<String> queue) {
        for (String suffix : JBOSS_LOGGING_SUFFIXES) {
            String companion = name + suffix;
            if (input.depBytecode.containsKey(companion) && visited.add(companion)) {
                queue.add(companion);
            }
        }
    }

    /**
     * If this class is a service interface, queue all its providers.
     * This handles indirect ServiceLoader.load() calls through helper methods
     * (e.g. jakarta.ws.rs FactoryFinder) that the direct call tracing can't detect.
     */
    private void enqueueServiceProviders(String name, Set<String> visited, Queue<String> queue) {
        Set<String> providers = input.serviceProviders.get(name);
        if (providers != null) {
            for (String provider : providers) {
                if (visited.add(provider)) {
                    queue.add(provider);
                }
            }
        }
    }

    /**
     * Result of the single-pass BFS bytecode scan. Captures all information
     * previously extracted by separate ASM passes.
     */
    private static class BfsScanResult {
        /** All class references discovered from bytecode (superclass, interfaces, fields, methods, annotations, etc.) */
        final Set<String> refs = new HashSet<>();
        /** Service interfaces loaded via {@code ServiceLoader.load(SomeClass.class)} patterns */
        final Set<String> serviceLoaderServices = new HashSet<>();
        /** Whether both sisu named resource string and getResources() call were detected */
        boolean sisuDetected;
        /** Flags for resource access and ObjectInputStream usage detection */
        int deserializationFlags;

        /** Resets all fields for reuse with the next class, avoiding per-class allocation. */
        void clear() {
            refs.clear();
            serviceLoaderServices.clear();
            sisuDetected = false;
            deserializationFlags = 0;
        }
    }

    /**
     * Single-pass ASM scan that combines reference extraction, string class matching,
     * ServiceLoader detection, sisu detection, resource/ObjectInputStream detection,
     * and caller-index building for {@link ClassLoadingChainAnalyzer}.
     */
    private void scanBytecode(String className, byte[] bytecode,
            boolean sisuAlreadyActivated, BfsScanResult result) {
        String internalOwner = className.replace('.', '/');
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(new RefCollectingClassVisitor(result.refs) {

            // Sisu detection state (class-level, across methods)
            boolean hasSisuString;
            boolean hasGetResources;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                visitMethodSignature(descriptor, signature, exceptions);
                // Capture the enclosing method's name/descriptor before they are
                // shadowed by the inner visitor's visitMethodInsn parameters
                final String mName = name;
                final String mDesc = descriptor;
                return new RefCollectingMethodVisitor(refs) {
                    // Lazily constructed MethodKey for this method, built only
                    // when a caller index entry is actually recorded
                    private MethodKey callerKeyObj;

                    private MethodKey callerKey() {
                        MethodKey k = callerKeyObj;
                        if (k == null) {
                            k = callerKeyObj = new MethodKey(
                                    internalOwner, mName, mDesc);
                        }
                        return k;
                    }

                    // State for ServiceLoader.load(SomeClass.class) tracking
                    private org.objectweb.asm.Type lastClassConstant;

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        super.visitTypeInsn(opcode, type);
                        lastClassConstant = null;
                        // Resource/OIS detection: NEW java/io/ObjectInputStream
                        if ("java/io/ObjectInputStream".equals(type)) {
                            result.deserializationFlags |= FLAG_OBJECT_INPUT_STREAM;
                        }
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                            String descriptor, boolean isInterface) {
                        // ServiceLoader.load(SomeClass.class)
                        if (SERVICE_LOADER_INTERNAL.equals(owner) && "load".equals(name)
                                && lastClassConstant != null) {
                            result.serviceLoaderServices
                                    .add(toClassName(lastClassConstant.getInternalName()));
                        }

                        // Resource/OIS detection
                        if ("getResourceAsStream".equals(name) || "getResource".equals(name)) {
                            result.deserializationFlags |= FLAG_RESOURCE_ACCESS;
                        }
                        if ("java/io/ObjectInputStream".equals(owner)) {
                            result.deserializationFlags |= FLAG_OBJECT_INPUT_STREAM;
                        }

                        // Sisu detection
                        if (!sisuAlreadyActivated
                                && ("getResources".equals(name) || "getResource".equals(name))) {
                            hasGetResources = true;
                        }

                        // Caller index for class-loading chain analysis.
                        // Pre-filter on owner prefix to avoid MethodKey construction
                        // for the ~90% of calls targeting JDK/infra classes.
                        if (!MethodKey.isJdkOrInfraClass(owner)) {
                            var calleeKey = new MethodKey(
                                    owner, name, descriptor);
                            callerIndex.computeIfAbsent(calleeKey, k -> new HashSet<>())
                                    .add(callerKey());
                        } else if (MethodKey.isSeedMethodOwner(owner)) {
                            var calleeKey = new MethodKey(
                                    owner, name, descriptor);
                            if (MethodKey.SEED_METHOD_KEYS.contains(calleeKey)) {
                                callerIndex.computeIfAbsent(calleeKey, k -> new HashSet<>())
                                        .add(callerKey());
                            }
                        }

                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        super.visitLdcInsn(value);
                        if (value instanceof String str) {
                            // String class references (for non-generated classes)
                            if (!input.generatedBytecode.containsKey(className)) {
                                matchClassNames(str, input.allKnownClasses, refs);
                            }
                            // Sisu detection
                            if (!sisuAlreadyActivated && SISU_NAMED_RESOURCE.equals(str)) {
                                hasSisuString = true;
                            }
                        }
                        if (value instanceof org.objectweb.asm.Type type) {
                            if (type.getSort() == org.objectweb.asm.Type.OBJECT) {
                                lastClassConstant = type;
                            } else {
                                lastClassConstant = null;
                            }
                        } else {
                            lastClassConstant = null;
                        }
                    }

                    @Override
                    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                        super.visitTryCatchBlock(start, end, handler, type);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        super.visitInsn(opcode);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        super.visitIntInsn(opcode, operand);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        super.visitJumpInsn(opcode, label);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String iname, String idescriptor,
                            Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        super.visitInvokeDynamicInsn(iname, idescriptor, bootstrapMethodHandle,
                                bootstrapMethodArguments);
                        lastClassConstant = null;
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        super.visitMultiANewArrayInsn(descriptor, numDimensions);
                        lastClassConstant = null;
                    }

                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        lastClassConstant = null;
                        return super.visitAnnotation(descriptor, visible);
                    }

                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter,
                            String descriptor, boolean visible) {
                        lastClassConstant = null;
                        return super.visitParameterAnnotation(parameter, descriptor, visible);
                    }
                };
            }

            @Override
            public void visitEnd() {
                // Sisu: both conditions must be true within the same class
                if (!sisuAlreadyActivated && hasSisuString && hasGetResources) {
                    result.sisuDetected = true;
                }
            }
        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }

    /**
     * Adds all classes from the given set to the BFS queue, skipping those already visited.
     */
    private static void enqueueNotVisited(Set<String> classes, Set<String> visited, Queue<String> queue) {
        for (String cls : classes) {
            if (visited.add(cls)) {
                queue.add(cls);
            }
        }
    }

    /**
     * Bit flags used as a heuristic to detect dependencies that may deserialize objects from
     * bundled resources. When a dependency's classes both access resources
     * ({@link #FLAG_RESOURCE_ACCESS}) and use {@code ObjectInputStream}
     * ({@link #FLAG_OBJECT_INPUT_STREAM}), it is assumed the dependency may deserialize
     * objects from bundled resources. In that case, serialized resource files are scanned
     * for class names to add as reachability roots. {@link #FLAG_SCANNED} prevents
     * redundant rescanning.
     */
    /** Dependency class calls {@code getResource()} or {@code getResourceAsStream()}. */
    private static final int FLAG_RESOURCE_ACCESS = 1;
    /** Dependency class instantiates or invokes methods on {@code java.io.ObjectInputStream}. */
    private static final int FLAG_OBJECT_INPUT_STREAM = 2;
    /** Both resource access and ObjectInputStream usage detected — triggers serialized resource scanning. */
    private static final int FLAG_RESOURCE_DESERIALIZATION = FLAG_RESOURCE_ACCESS | FLAG_OBJECT_INPUT_STREAM;
    /** Dependency's serialized resources have already been scanned — skip on subsequent classes. */
    private static final int FLAG_SCANNED = 4;

    /**
     * Scans non-class resources in the given dependency for Java serialization streams
     * (magic bytes {@code 0xACED0005}) and extracts class names from class descriptors.
     */
    private static Set<String> extractClassNamesFromSerializedResources(OpenPathTree openTree) {
        Set<String> classNames = new HashSet<>();
        openTree.walk(visit -> {
            String entry = visit.getResourceName();
            if (entry.endsWith(".class") || entry.endsWith("/") || entry.startsWith("META-INF/")) {
                return;
            }
            try (InputStream is = Files.newInputStream(visit.getPath())) {
                byte[] header = new byte[4];
                if (is.read(header) == 4
                        && header[0] == (byte) 0xAC && header[1] == (byte) 0xED
                        && header[2] == (byte) 0x00 && header[3] == (byte) 0x05) {
                    byte[] rest = is.readAllBytes();
                    extractClassDescriptorNames(rest, classNames);
                }
            } catch (IOException e) {
                // ignore unreadable resources
            }
        });
        return classNames;
    }

    /**
     * Parses Java serialization stream data for TC_CLASSDESC markers ({@code 0x72})
     * and extracts the class names from the UTF-encoded class descriptor strings.
     */
    private static void extractClassDescriptorNames(byte[] data, Set<String> classNames) {
        for (int i = 0; i < data.length - 2; i++) {
            if (data[i] == (byte) 0x72) {
                int len = ((data[i + 1] & 0xFF) << 8) | (data[i + 2] & 0xFF);
                if (len > 0 && len < 500 && i + 3 + len <= data.length) {
                    String name = new String(data, i + 3, len, StandardCharsets.UTF_8);
                    if (isValidClassName(name)) {
                        classNames.add(name);
                    }
                    i += 2 + len; // skip past the class name
                }
            }
        }
    }

    /**
     * Validates that a string looks like a fully-qualified Java class name
     * (contains at least one dot, starts with a valid identifier character,
     * and contains only valid identifier characters, dots, and dollar signs).
     */
    private static boolean isValidClassName(String name) {
        if (name.isEmpty() || name.length() > 300) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        boolean hasDot = false;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                hasDot = true;
            } else if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return hasDot;
    }

    // ---- Bytecode analysis ----

    /**
     * Extracts all class references from bytecode using ASM: superclass, interfaces,
     * field/method descriptors, generic signatures, annotations, method body instructions
     * (type insns, field/method owners, LDC class constants, invokedynamic handles),
     * and {@code Class.forName()}/{@code ClassLoader.loadClass()} calls with string constants.
     */
    private Set<String> extractReferencesFromBytecode(byte[] bytecode) {
        final Set<String> refs = new HashSet<>();
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(new RefCollectingClassVisitor(refs), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return refs;
    }

    /**
     * Extracts LDC string constants from bytecode that match known class names.
     * This catches class names passed as string arguments to methods that eventually
     * call Class.forName() at runtime (e.g. Quarkus recorder generated code).
     * Also handles delimited class name lists (comma, colon) used by frameworks
     * like RESTEasy for provider and resource builder lists.
     */
    static Set<String> extractStringClassReferences(byte[] bytecode, Set<String> knownClasses) {
        final Set<String> refs = new HashSet<>();
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String str) {
                            matchClassNames(str, knownClasses, refs);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return refs;
    }

    /**
     * Checks whether a string (or its comma/colon-delimited parts) matches known class names,
     * adding matches to {@code refs}. Uses manual character-level splitting to avoid
     * the per-call {@link java.util.regex.Pattern} compilation that {@link String#split}
     * incurs for the {@code [,:]} character class.
     */
    static void matchClassNames(String str, Set<String> knownClasses, Set<String> refs) {
        if (str.indexOf(',') >= 0 || str.indexOf(':') >= 0) {
            int start = 0;
            for (int i = 0; i <= str.length(); i++) {
                if (i == str.length() || str.charAt(i) == ',' || str.charAt(i) == ':') {
                    if (i > start) {
                        String part = str.substring(start, i);
                        if (knownClasses.contains(part)) {
                            refs.add(part);
                        }
                    }
                    start = i + 1;
                }
            }
        } else {
            if (knownClasses.contains(str)) {
                refs.add(str);
            }
        }
    }

    /**
     * Creates an AnnotationVisitor that extracts class references from annotation values.
     * Handles class literals (e.g. {@code @Command(subcommands = {Foo.class})}),
     * enum constants, nested annotations, and arrays of these.
     */
    private static org.objectweb.asm.AnnotationVisitor createAnnotationRefVisitor(Set<String> refs) {
        return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if (value instanceof org.objectweb.asm.Type) {
                    addAsmType((org.objectweb.asm.Type) value, refs);
                }
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                addDescriptorType(descriptor, refs);
            }

            @Override
            public org.objectweb.asm.AnnotationVisitor visitAnnotation(String name, String descriptor) {
                addDescriptorType(descriptor, refs);
                return this;
            }

            @Override
            public org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                return this;
            }
        };
    }

    // ---- ASM visitor base classes ----

    /**
     * ClassVisitor that extracts all class references from bytecode: superclass, interfaces,
     * field/method descriptors, generic signatures, annotations, and inner-to-outer links.
     * Returns a {@link RefCollectingMethodVisitor} from {@link #visitMethod} for instruction-level
     * reference extraction.
     */
    private static class RefCollectingClassVisitor extends ClassVisitor {
        final Set<String> refs;

        RefCollectingClassVisitor(Set<String> refs) {
            super(Opcodes.ASM9);
            this.refs = refs;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            if (superName != null) {
                addClassRef(superName, refs);
            }
            if (interfaces != null) {
                for (String iface : interfaces) {
                    addClassRef(iface, refs);
                }
            }
            addSignatureTypes(signature, refs);
            int dollar = name.lastIndexOf('$');
            if (dollar > 0) {
                addClassRef(name.substring(0, dollar), refs);
            }
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addDescriptorType(descriptor, refs);
            return createAnnotationRefVisitor(refs);
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                String descriptor, String signature, Object value) {
            addDescriptorType(descriptor, refs);
            addSignatureTypes(signature, refs);
            return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    addDescriptorType(descriptor, refs);
                    return createAnnotationRefVisitor(refs);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            visitMethodSignature(descriptor, signature, exceptions);
            return new RefCollectingMethodVisitor(refs);
        }

        /** Adds method descriptor types, generic signature types, and exception types to refs. */
        final void visitMethodSignature(String descriptor, String signature, String[] exceptions) {
            addMethodDescriptorTypes(descriptor, refs);
            addSignatureTypes(signature, refs);
            if (exceptions != null) {
                for (String ex : exceptions) {
                    addClassRef(ex, refs);
                }
            }
        }
    }

    /**
     * MethodVisitor that extracts class references from instructions: type insns, field/method
     * owners, descriptors, LDC class constants, invokedynamic handles, try-catch types, and
     * annotations. Tracks {@code lastStringConstant} to detect {@code Class.forName()} and
     * {@code ClassLoader.loadClass()} calls with string arguments.
     */
    private static class RefCollectingMethodVisitor extends MethodVisitor {
        final Set<String> refs;
        String lastStringConstant;

        RefCollectingMethodVisitor(Set<String> refs) {
            super(Opcodes.ASM9);
            this.refs = refs;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            lastStringConstant = null;
            if (type != null) {
                addClassRef(type, refs);
            }
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotationDefault() {
            return createAnnotationRefVisitor(refs);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            lastStringConstant = null;
            addClassRef(type, refs);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            lastStringConstant = null;
            addClassRef(owner, refs);
            addDescriptorType(descriptor, refs);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String descriptor, boolean isInterface) {
            if (lastStringConstant != null) {
                if (("java/lang/Class".equals(owner) && "forName".equals(name))
                        || "loadClass".equals(name)) {
                    refs.add(lastStringConstant);
                }
            }
            lastStringConstant = null;
            addClassRef(owner, refs);
            addMethodDescriptorTypes(descriptor, refs);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof String str) {
                lastStringConstant = str;
            } else {
                lastStringConstant = null;
            }
            if (value instanceof org.objectweb.asm.Type type) {
                if (type.getSort() == org.objectweb.asm.Type.OBJECT) {
                    addClassRef(type.getInternalName(), refs);
                } else if (type.getSort() == org.objectweb.asm.Type.ARRAY) {
                    org.objectweb.asm.Type elem = type.getElementType();
                    if (elem.getSort() == org.objectweb.asm.Type.OBJECT) {
                        addClassRef(elem.getInternalName(), refs);
                    }
                }
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode != Opcodes.ICONST_0 && opcode != Opcodes.ICONST_1) {
                lastStringConstant = null;
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            // Don't reset lastStringConstant — BIPUSH/SIPUSH between LDC and Class.forName
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (opcode != Opcodes.ALOAD) {
                lastStringConstant = null;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            lastStringConstant = null;
        }

        @Override
        public void visitInvokeDynamicInsn(String iname, String idescriptor,
                Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            lastStringConstant = null;
            addHandleType(bootstrapMethodHandle, refs);
            if (bootstrapMethodArguments != null) {
                for (Object arg : bootstrapMethodArguments) {
                    if (arg instanceof org.objectweb.asm.Type type) {
                        addAsmType(type, refs);
                    } else if (arg instanceof Handle) {
                        addHandleType((Handle) arg, refs);
                    }
                }
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            lastStringConstant = null;
            addDescriptorType(descriptor, refs);
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            lastStringConstant = null;
            addDescriptorType(descriptor, refs);
            return createAnnotationRefVisitor(refs);
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter,
                String descriptor, boolean visible) {
            lastStringConstant = null;
            addDescriptorType(descriptor, refs);
            return createAnnotationRefVisitor(refs);
        }
    }

    // ---- Helpers ----

    /**
     * Converts a JVM internal name (e.g. {@code com/example/Foo}) to a dot-separated
     * class name (e.g. {@code com.example.Foo}).
     */
    private static String toClassName(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * Adds a class reference to {@code refs} after converting from JVM internal name
     * to dot-separated format, but only if the class is not in the {@code java/}
     * package hierarchy. JDK classes are never present in the dependency, application,
     * or generated bytecode maps, so they are always filtered out downstream by
     * {@link JarTreeShakeInput#hasBytecode} — skipping them here avoids the
     * {@link String#replace} allocation and the {@link Set#add} lookup.
     */
    private static void addClassRef(String internalName, Set<String> refs) {
        if (!internalName.startsWith("java/")) {
            refs.add(internalName.replace('/', '.'));
        }
    }

    /**
     * Returns {@code true} if the descriptor references at least one object type.
     * In JVM descriptor grammar, object types are encoded as {@code Lfully/qualified/Name;},
     * so the presence of the {@code L} character is a necessary and sufficient condition.
     * Descriptors that contain only primitive types ({@code I}, {@code J}, {@code D}, etc.),
     * void ({@code V}), or arrays of primitives ({@code [I}, {@code [B}) will not
     * contain {@code L}.
     */
    private static boolean hasObjectType(String descriptor) {
        return descriptor.indexOf('L') >= 0;
    }

    /**
     * Parses a single field or type descriptor (e.g. {@code Ljava/lang/String;})
     * and adds any object type reference to {@code refs}. Skips parsing when the
     * descriptor contains only primitive types, void, or arrays of primitives.
     */
    private static void addDescriptorType(String descriptor, Set<String> refs) {
        if (!hasObjectType(descriptor)) {
            return;
        }
        org.objectweb.asm.Type type = org.objectweb.asm.Type.getType(descriptor);
        addAsmType(type, refs);
    }

    /**
     * Parses a method descriptor (e.g. {@code (Ljava/lang/String;I)V}) and adds
     * all argument and return type object references to {@code refs}. Skips
     * parsing when the descriptor contains only primitive types and void.
     */
    private static void addMethodDescriptorTypes(String descriptor, Set<String> refs) {
        if (!hasObjectType(descriptor)) {
            return;
        }
        for (org.objectweb.asm.Type argType : org.objectweb.asm.Type.getArgumentTypes(descriptor)) {
            addAsmType(argType, refs);
        }
        addAsmType(org.objectweb.asm.Type.getReturnType(descriptor), refs);
    }

    /**
     * Adds the owner class reference and descriptor type references from an
     * {@code invokedynamic} bootstrap method handle to {@code refs}.
     */
    private static void addHandleType(Handle handle, Set<String> refs) {
        addClassRef(handle.getOwner(), refs);
        int tag = handle.getTag();
        if (tag == Opcodes.H_GETFIELD || tag == Opcodes.H_GETSTATIC
                || tag == Opcodes.H_PUTFIELD || tag == Opcodes.H_PUTSTATIC) {
            addDescriptorType(handle.getDesc(), refs);
        } else {
            addMethodDescriptorTypes(handle.getDesc(), refs);
        }
    }

    /**
     * Parses a generic signature and extracts all class type references.
     * This captures type arguments (e.g. {@code Map<String, ContainerConfig>})
     * that are not present in raw descriptors but can be resolved at runtime
     * via reflection on generic type metadata.
     */
    private static void addSignatureTypes(String signature, Set<String> refs) {
        if (signature == null) {
            return;
        }
        new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
            @Override
            public void visitClassType(String name) {
                addClassRef(name, refs);
            }
        });
    }

    /**
     * Adds the class reference from an ASM {@link org.objectweb.asm.Type} to {@code refs},
     * recursing into the element type for array types. Delegates to {@link #addClassRef}
     * to skip {@code java/} package classes.
     */
    private static void addAsmType(org.objectweb.asm.Type type, Set<String> refs) {
        if (type.getSort() == org.objectweb.asm.Type.OBJECT) {
            addClassRef(type.getInternalName(), refs);
        } else if (type.getSort() == org.objectweb.asm.Type.ARRAY) {
            addAsmType(type.getElementType(), refs);
        }
    }

    private static long bytecodeSize(Supplier<byte[]> supplier) {
        if (supplier instanceof JarTreeShakeInput.BytecodeSupplier bs) {
            return bs.size;
        }
        // Transformed classes use lambda suppliers wrapping byte[]
        byte[] data = supplier.get();
        return data != null ? data.length : 0;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

}
