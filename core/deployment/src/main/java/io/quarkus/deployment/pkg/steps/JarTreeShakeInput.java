package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeConditionalRootClassBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeDeclaredClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeExcludedArtifactBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootBytecodeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleProvideInfo;
import io.smallrye.classfile.constantpool.ClassEntry;

/**
 * Assembles all data needed by {@link JarTreeShaker} for reachability analysis.
 * Separates input collection (dependency walking, build item extraction, root identification)
 * from the BFS analysis itself.
 *
 * <p>
 * Implements {@link AutoCloseable} because it owns {@link OpenPathTree} instances
 * whose lifecycle must extend until the analysis completes (bytecode suppliers hold
 * {@link Path} references into the open trees).
 *
 * <p>
 * Use the {@link #collect} factory method to build an instance from the application model
 * and build items, then pass it to {@link JarTreeShaker}.
 */
class JarTreeShakeInput implements AutoCloseable {

    private static final Logger log = Logger.getLogger(JarTreeShakeInput.class.getName());

    private static final String SISU_NAMED_RESOURCE = "META-INF/sisu/javax.inject.Named";
    private static final String META_INF_VERSIONS_PREFIX = "META-INF/versions/";
    private static final String META_INF_SERVICES = "META-INF/services/";
    private static final String MODULE_INFO_CLASS = "module-info.class";

    /** Configured tree-shake mode controlling analysis aggressiveness. */
    final TreeShakeConfig.TreeShakeMode treeShakeMode;
    /** Dot-separated class names that are unconditionally reachable (BFS starting points). */
    final Set<String> roots = new HashSet<>();
    /**
     * Map from condition class to classes that become roots when the condition is reachable.
     * Consumed by the analysis: {@link JarTreeShaker} removes an entry when its condition class is reached,
     * so the map must be mutable. Once all reachable classes have been traced, it holds only the conditions
     * that were never met, until {@link #releaseAnalysisData()} clears it.
     */
    final Map<String, Set<String>> conditionalRoots = new HashMap<>();
    /** Classes listed in {@code META-INF/sisu/javax.inject.Named} files. */
    final Set<String> sisuNamedClasses = new HashSet<>();
    /** Bytecode suppliers for runtime dependency classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> depBytecode = new HashMap<>();
    /** Bytecode suppliers for application classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> appBytecode = new HashMap<>();
    /** Bytecode suppliers for build-time generated classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> generatedBytecode = new HashMap<>();
    /** Maps dot-separated dependency class names to their originating artifact. */
    final Map<String, ArtifactKey> classToDep = new HashMap<>();
    /** Service interface to provider class names parsed from {@code META-INF/services/} files. */
    final Map<String, Set<String>> serviceProviders = new HashMap<>();
    /** Service interface to consumer class names detected via {@code ServiceLoader.load()} calls. */
    final Map<String, Set<String>> serviceLoaderCalls = new HashMap<>();
    /** Union of dependency and generated class names, used for string-constant reference matching. */
    final Set<String> allKnownClasses = new HashSet<>();
    /** Java feature version detected from application bytecode (e.g. 17, 21). */
    final int appJavaVersion;
    /** Open content trees for dependencies, kept alive until {@link #close()}. */
    final Map<ArtifactKey, OpenPathTree> openTrees = new HashMap<>();
    /** Filesystem paths of resolved runtime dependency JARs. */
    final List<Path> depJarPaths = new ArrayList<>();
    /** Filesystem paths of the application artifact. */
    final List<Path> appPaths = new ArrayList<>();
    /** Class names whose bytecode was replaced by a build-time transformation. */
    final Set<String> transformedClassNames = new HashSet<>();
    /**
     * Bytecode from multi-release entries targeting Java versions newer than {@code appJavaVersion}.
     * Keyed by dot-separated class name. These are not used for the main reachability analysis
     * but their references must be traced after analysis to ensure classes they depend on
     * are also marked reachable.
     */
    final Map<String, List<Supplier<byte[]>>> higherVersionBytecode = new HashMap<>();
    /** Artifacts excluded from tree-shaking via {@link JarTreeShakeExcludedArtifactBuildItem}. */
    final Set<ArtifactKey> excludedArtifacts = new HashSet<>();
    /**
     * Map from condition class to classes whose declared classes must be loadable when the condition is reachable,
     * from {@link JarTreeShakeDeclaredClassesBuildItem}. Consumed by the analysis like {@link #conditionalRoots}.
     */
    final Map<String, Set<String>> conditionalDeclaredClasses = new HashMap<>();
    /** Classes whose declared classes must be loadable regardless of other classes. */
    final Set<String> declaredClassesHolders = new HashSet<>();

    /**
     * Creates an empty tree-shake input, whose collections are mutable and are filled by {@link #collect}
     * or, in tests, directly.
     *
     * @param treeShakeMode configured tree-shake mode
     * @param appJavaVersion Java feature version detected from application bytecode
     */
    JarTreeShakeInput(TreeShakeConfig.TreeShakeMode treeShakeMode, int appJavaVersion) {
        this.treeShakeMode = treeShakeMode;
        this.appJavaVersion = appJavaVersion;
    }

    /**
     * Closes all {@link OpenPathTree} instances opened during input collection.
     */
    @Override
    public void close() {
        for (OpenPathTree openPathTree : openTrees.values()) {
            try {
                openPathTree.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Looks up the bytecode supplier for the given class name, checking generated,
     * dependency, and application bytecode maps in priority order.
     */
    Supplier<byte[]> getBytecode(String name) {
        Supplier<byte[]> s = generatedBytecode.get(name);
        if (s != null) {
            return s;
        }
        s = depBytecode.get(name);
        if (s != null) {
            return s;
        }
        return appBytecode.get(name);
    }

    /**
     * Returns true if bytecode is available for the given class name
     * in any of the three bytecode maps.
     */
    boolean hasBytecode(String name) {
        return generatedBytecode.containsKey(name)
                || depBytecode.containsKey(name)
                || appBytecode.containsKey(name);
    }

    /**
     * Reads bytecode for the given class.
     * The returned byte[] is the only reference; once the caller drops it, the
     * bytecode is eligible for GC. The supplier can re-read from disk if needed later.
     */
    byte[] readBytecode(String name) {
        Supplier<byte[]> supplier = getBytecode(name);
        return supplier == null ? null : supplier.get();
    }

    /**
     * Factory method that assembles all input for the tree-shake analysis.
     * Walks dependency and application JARs, collects bytecode, service provider metadata,
     * and extracts root classes from generated classes, native-image build items,
     * and string-constant class references.
     *
     * <p>
     * The caller must close the returned instance to release the opened dependency trees.
     * If collection fails, the trees opened so far are closed before the exception is propagated.
     */
    static JarTreeShakeInput collect(
            TreeShakeConfig.TreeShakeMode treeShakeMode,
            ApplicationModel appModel,
            List<GeneratedClassBuildItem> generatedClasses,
            TransformedClassesBuildItem transformedClasses,
            List<ReflectiveClassConditionBuildItem> reflectiveClassConditions,
            List<JarTreeShakeRootClassBuildItem> rootClasses,
            List<JarTreeShakeConditionalRootClassBuildItem> conditionalRootClasses,
            List<JarTreeShakeRootBytecodeBuildItem> rootBytecode,
            List<JarTreeShakeDeclaredClassesBuildItem> declaredClasses,
            List<JarTreeShakeExcludedArtifactBuildItem> excludedArtifactItems) {

        final JarTreeShakeInput input = new JarTreeShakeInput(treeShakeMode, detectAppJavaVersion(appModel));
        try {
            input.collectGeneratedClasses(generatedClasses);
            input.collectRuntimeClasses(appModel);
            input.collectTransformedClasses(transformedClasses);
            input.collectApplicationClasses(appModel);
            for (Path p : appModel.getAppArtifact().getResolvedPaths()) {
                input.appPaths.add(p);
            }

            input.addServiceProviderRoots();

            input.collectAllKnownClasses();
            input.addStringClassReferenceRoots();

            for (JarTreeShakeRootClassBuildItem item : rootClasses) {
                input.roots.add(item.getClassName());
            }
            for (JarTreeShakeRootBytecodeBuildItem item : rootBytecode) {
                final byte[] bytecode = item.getBytecode();
                input.appBytecode.put(item.getClassName(), () -> bytecode);
                input.roots.add(item.getClassName());
            }

            input.collectConditionalRoots(reflectiveClassConditions);
            input.collectExcludedArtifacts(excludedArtifactItems);
            final Predicate<String> reachabilityAnalyzed = reachabilityAnalyzed(input.depBytecode, input.classToDep,
                    input.excludedArtifacts);
            final Predicate<String> alwaysPresent = alwaysPresent(input.depBytecode, input.appBytecode,
                    input.generatedBytecode);
            addConditionalRoots(conditionalRootClasses, reachabilityAnalyzed, alwaysPresent, input.roots,
                    input.conditionalRoots);
            for (JarTreeShakeDeclaredClassesBuildItem item : declaredClasses) {
                addConditional(item.getConditionClassName(), item.getHolderClassName(), reachabilityAnalyzed, alwaysPresent,
                        input.declaredClassesHolders, input.conditionalDeclaredClasses);
            }
            return input;
        } catch (RuntimeException | Error e) {
            // the caller only closes the input once it's returned
            input.close();
            throw e;
        }
    }

    /**
     * Walks all runtime classpath dependencies to collect bytecode (with multi-release resolution),
     * class-to-dependency mappings, service provider files, and sisu named components.
     * Classes from {@code quarkus-bootstrap-runner} are added as roots.
     */
    private void collectRuntimeClasses(ApplicationModel appModel) {
        final Map<String, Integer> depBytecodeVersion = new HashMap<>();
        for (ResolvedDependency dep : appModel.getDependencies(DependencyFlags.RUNTIME_CP)) {
            final boolean addClassesAsRoots = "quarkus-bootstrap-runner".equals(dep.getArtifactId());

            for (Path p : dep.getResolvedPaths()) {
                depJarPaths.add(p);
            }
            final ModuleDescriptorSelector moduleDescriptors = new ModuleDescriptorSelector(appJavaVersion, serviceProviders);
            openPathTree(dep).walkRaw(visit -> {
                String entry = visit.getResourceName();
                if (isModuleInfoEntry(entry)) {
                    moduleDescriptors.add(entry, visit.getPath());
                    return;
                }
                if (isClassEntry(entry)) {
                    processRuntimeClassEntry(entry, visit.getPath(), dep, addClassesAsRoots, depBytecodeVersion);
                    return;
                }
                if (entry.startsWith(META_INF_SERVICES) && !entry.endsWith("/")) {
                    parseServiceFile(visit.getPath(), entry, serviceProviders);
                    return;
                }
                if (SISU_NAMED_RESOURCE.equals(entry)) {
                    parseSisuNamedFile(visit.getPath(), sisuNamedClasses);
                }
            });
            moduleDescriptors.complete();
        }
    }

    /**
     * Processes a single class entry from a runtime dependency JAR. Handles multi-release
     * resolution (keeping the highest version &le; {@code appJavaVersion}), records the
     * bytecode supplier and class-to-dependency mapping, and optionally adds the class as a root.
     */
    private void processRuntimeClassEntry(
            String entry,
            Path path,
            ResolvedDependency dep,
            boolean addClassesAsRoots,
            Map<String, Integer> depBytecodeVersion) {

        final int classJavaVersion = multiReleaseVersion(entry);
        if (classJavaVersion < 0) {
            // not a valid multi-release version directory, the JVM never loads it
            return;
        }
        final String className = classJavaVersion == 0 ? classNameOf(entry)
                : classNameOf(entry, entry.indexOf('/', META_INF_VERSIONS_PREFIX.length()) + 1);

        if (classJavaVersion > appJavaVersion) {
            // Multi-release entry for a Java version newer than appJavaVersion.
            // Collect its bytecode so references can be traced after the main analysis
            // for classes that are reachable. Also register in classToDep so the class
            // is part of allKnownClasses (needed for reference matching).
            higherVersionBytecode.computeIfAbsent(className, k -> new ArrayList<>())
                    .add(new BytecodeSupplier(path));
            classToDep.putIfAbsent(className, dep.getKey());
            return;
        }

        int currentVersion = depBytecodeVersion.getOrDefault(className, -1);
        if (currentVersion < 0) {
            classToDep.put(className, dep.getKey());
        }
        if (classJavaVersion > currentVersion) {
            depBytecodeVersion.put(className, classJavaVersion);
            depBytecode.put(className, new BytecodeSupplier(path));
        }

        if (addClassesAsRoots) {
            roots.add(className);
        }
    }

    /**
     * Returns the Java version a JAR entry targets: {@code 0} for an entry outside of
     * {@code META-INF/versions/}, {@code N} for an entry under {@code META-INF/versions/N/}
     * or {@code -1} for an entry under a version directory the JVM ignores, i.e. one that isn't
     * a number or is below 9, the first release supporting multi-release JARs.
     */
    static int multiReleaseVersion(String entry) {
        if (!entry.startsWith(META_INF_VERSIONS_PREFIX)) {
            return 0;
        }
        final int javaVersionSeparator = entry.indexOf('/', META_INF_VERSIONS_PREFIX.length());
        if (javaVersionSeparator == -1) {
            return -1;
        }
        try {
            int version = Integer.parseInt(entry.substring(META_INF_VERSIONS_PREFIX.length(), javaVersionSeparator));
            return version < 9 ? -1 : version;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Overrides dependency bytecode with transformed bytecode where available.
     * Transformations can add interfaces, annotations, or other references
     * that aren't in the original bytecode (e.g. NettySharable marker interface).
     */
    private void collectTransformedClasses(TransformedClassesBuildItem transformedClasses) {
        for (Set<TransformedClassesBuildItem.TransformedClass> transformedSet : transformedClasses
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass tc : transformedSet) {
                if (tc.getData() != null) {
                    String fileName = tc.getFileName();
                    if (isClassEntry(fileName)) {
                        String className = classNameOf(fileName);
                        depBytecode.put(className, tc::getData);
                        transformedClassNames.add(className);
                    }
                }
            }
        }
    }

    /**
     * Collects bytecode and service providers from the application artifact.
     * App bytecode is needed so the reachability trace can follow references from app classes
     * to dependency classes (e.g. app code using a library utility class).
     */
    private void collectApplicationClasses(ApplicationModel appModel) {
        final ModuleDescriptorSelector moduleDescriptors = new ModuleDescriptorSelector(appJavaVersion, serviceProviders);
        openPathTree(appModel.getAppArtifact()).walk(visit -> {
            String entry = visit.getResourceName();
            if (isModuleInfoEntry(entry)) {
                moduleDescriptors.add(entry, visit.getPath());
                return;
            }
            if (isClassEntry(entry)) {
                appBytecode.put(classNameOf(entry), new BytecodeSupplier(visit.getPath()));
                return;
            }
            if (entry.startsWith(META_INF_SERVICES) && !entry.endsWith("/")) {
                parseServiceFile(visit.getPath(), entry, serviceProviders);
            }
        });
        moduleDescriptors.complete();
    }

    /**
     * Add service providers for JDK service interfaces as roots, since JDK code
     * isn't analyzed and its ServiceLoader.load() calls can't be traced.
     * Non-JDK service providers are discovered through ServiceLoader.load() call
     * tracing in reachable dependency/app code.
     */
    private void addServiceProviderRoots() {
        for (Map.Entry<String, Set<String>> entry : serviceProviders.entrySet()) {
            String serviceInterface = entry.getKey();
            if (!classToDep.containsKey(serviceInterface) && !generatedBytecode.containsKey(serviceInterface)) {
                roots.addAll(entry.getValue());
            }
        }
    }

    /**
     * Collects the union of dependency and generated class names, used for string-constant
     * class reference matching.
     */
    private void collectAllKnownClasses() {
        allKnownClasses.addAll(classToDep.keySet());
        allKnownClasses.addAll(generatedBytecode.keySet());
    }

    /**
     * Scan generated classes for LDC string constants that match known dependency class names.
     * Generated recorder bytecode often passes class names as strings to methods like
     * factory(className) that eventually call Class.forName() at runtime.
     */
    private void addStringClassReferenceRoots() {
        for (Supplier<byte[]> bytecode : generatedBytecode.values()) {
            Set<String> stringClassRefs = JarTreeShaker.extractStringClassReferences(bytecode.get(), allKnownClasses);
            for (String ref : stringClassRefs) {
                if (roots.add(ref)) {
                    log.debugf("String constant class reference from generated code: %s", ref);
                }
            }
        }
    }

    /**
     * Returns a predicate telling whether the analysis determines the reachability of a class:
     * true for dependency classes, except those of artifacts excluded from tree shaking, which are always kept.
     */
    static Predicate<String> reachabilityAnalyzed(
            Map<String, Supplier<byte[]>> depBytecode,
            Map<String, ArtifactKey> classToDep,
            Set<ArtifactKey> excludedArtifacts) {
        return name -> depBytecode.containsKey(name)
                && (excludedArtifacts.isEmpty() || !excludedArtifacts.contains(classToDep.get(name)));
    }

    /**
     * Returns a predicate telling whether a class is present at runtime regardless of the analysis:
     * dependency classes (to be combined with {@link #reachabilityAnalyzed}, so effectively those of excluded
     * artifacts), application and generated classes, which are never removed, and JDK and GraalVM classes.
     */
    static Predicate<String> alwaysPresent(
            Map<String, Supplier<byte[]>> depBytecode,
            Map<String, Supplier<byte[]>> appBytecode,
            Map<String, Supplier<byte[]>> generatedBytecode) {
        return name -> depBytecode.containsKey(name)
                || appBytecode.containsKey(name)
                || generatedBytecode.containsKey(name)
                || isPlatformClass(name);
    }

    /**
     * Package prefixes of JDK and GraalVM classes. The JVM running the build may not have all of them,
     * e.g. platform-specific JDK classes, classes of a newer JDK used by {@code native-image}, or
     * GraalVM builder classes, which aren't on the application class path.
     */
    private static final String[] PLATFORM_PACKAGE_PREFIXES = {
            "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.graalvm.", "com.oracle.svm.", "com.oracle.graal."
    };

    /**
     * Whether the class belongs to the JDK or GraalVM rather than to the application class path.
     */
    private static boolean isPlatformClass(String name) {
        for (String prefix : PLATFORM_PACKAGE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return ClassLoader.getPlatformClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }

    /**
     * Adds conditional roots from {@link JarTreeShakeConditionalRootClassBuildItem}s:
     * <ul>
     * <li>if the analysis determines the reachability of the condition class, the class is a conditional root;</li>
     * <li>otherwise, if the condition class is always present (e.g. a JDK class, or a class of an artifact excluded
     * from tree shaking), the class is an unconditional root;</li>
     * <li>otherwise the condition class isn't on the classpath, can never be reachable, and the class isn't a root.</li>
     * </ul>
     *
     * @param items conditional root build items
     * @param reachabilityAnalyzed whether the analysis determines the reachability of a class
     * @param alwaysPresent whether a class whose reachability isn't analyzed is present at runtime
     * @param roots unconditional roots to add to
     * @param conditionalRoots condition class to classes that become roots when the condition is reachable, to add to
     */
    static void addConditionalRoots(
            List<JarTreeShakeConditionalRootClassBuildItem> items,
            Predicate<String> reachabilityAnalyzed,
            Predicate<String> alwaysPresent,
            Set<String> roots,
            Map<String, Set<String>> conditionalRoots) {
        for (JarTreeShakeConditionalRootClassBuildItem item : items) {
            addConditional(item.getConditionClassName(), item.getClassName(), reachabilityAnalyzed, alwaysPresent,
                    roots, conditionalRoots);
        }
    }

    private static void addConditional(String condition, String className, Predicate<String> reachabilityAnalyzed,
            Predicate<String> alwaysPresent, Set<String> unconditional, Map<String, Set<String>> conditional) {
        if (reachabilityAnalyzed.test(condition)) {
            conditional.computeIfAbsent(condition, k -> new HashSet<>()).add(className);
        } else if (alwaysPresent.test(condition)) {
            unconditional.add(className);
        }
    }

    /**
     * Maps each condition type to the set of class names that should become roots
     * when that condition type is reachable. {@link JarTreeShaker} consumes an entry when its condition type
     * is reached.
     */
    private void collectConditionalRoots(List<ReflectiveClassConditionBuildItem> reflectiveClassConditions) {
        for (ReflectiveClassConditionBuildItem item : reflectiveClassConditions) {
            conditionalRoots
                    .computeIfAbsent(item.getTypeReached(), k -> new HashSet<>())
                    .add(item.getClassName());
        }
    }

    /**
     * Collects bytecode suppliers of {@link GeneratedClassBuildItem}s,
     * translating binary names (with {@code /}) to dot-separated class names.
     */
    private void collectGeneratedClasses(List<GeneratedClassBuildItem> generatedClasses) {
        for (GeneratedClassBuildItem gen : generatedClasses) {
            generatedBytecode.put(gen.binaryName().replace('/', '.'), gen::getClassData);
        }
    }

    private void collectExcludedArtifacts(List<JarTreeShakeExcludedArtifactBuildItem> excludedArtifactItems) {
        for (JarTreeShakeExcludedArtifactBuildItem item : excludedArtifactItems) {
            excludedArtifacts.add(item.getArtifactKey());
        }
    }

    // ---- Supporting methods ----

    /**
     * Opens a dependency's content tree and registers it for cleanup on {@link #close()}.
     */
    private OpenPathTree openPathTree(ResolvedDependency dep) {
        return openTrees.computeIfAbsent(dep.getKey(), k -> dep.getContentTree().open());
    }

    /**
     * Detects the Java feature version from the application's class bytecode.
     * The class file major version maps to Java versions: 52=8, 55=11, 61=17, 65=21.
     * Falls back to the current JVM's feature version if no app classes are found.
     */
    private static int detectAppJavaVersion(ApplicationModel appModel) {
        int[] majorVersion = new int[1];
        appModel.getAppArtifact().getContentTree().walk(visit -> {
            String entry = visit.getResourceName();
            if (isClassEntry(entry)) {
                try (InputStream is = Files.newInputStream(visit.getPath())) {
                    byte[] header = new byte[8];
                    if (is.read(header) == 8) {
                        majorVersion[0] = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                        if (majorVersion[0] > 0) {
                            visit.stopWalking();
                        }
                    }
                } catch (IOException e) {
                    // ignore, will try next class
                }
            }
        });
        int javaVersion = majorVersion[0] > 0 ? majorVersion[0] - 44 : Runtime.version().feature();
        log.debugf("Detected app Java version: %d (class file major: %d)", javaVersion, majorVersion[0]);
        return javaVersion;
    }

    static boolean isClassEntry(String resourceName) {
        return ClassLoaderHelper.isClassEntry(resourceName);
    }

    /**
     * Converts a class resource path (e.g. {@code com/example/Foo.class}) to a dot-separated
     * class name (e.g. {@code com.example.Foo}).
     */
    static String classNameOf(String classResourceName) {
        return classNameOf(classResourceName, 0);
    }

    /**
     * Converts a class resource path to a dot-separated class name, starting from
     * the given index (used for multi-release entries to skip the {@code META-INF/versions/N/} prefix).
     */
    static String classNameOf(String resourceName, int classNameStartIndex) {
        return resourceName.substring(classNameStartIndex, resourceName.length() - 6).replace('/', '.');
    }

    /**
     * Checks whether the entry is a module descriptor, either at the root
     * or in a multi-release {@code META-INF/versions/N/} directory.
     */
    static boolean isModuleInfoEntry(String resourceName) {
        final int descriptorStart = resourceName.length() - MODULE_INFO_CLASS.length();
        if (descriptorStart < 0 || !resourceName.startsWith(MODULE_INFO_CLASS, descriptorStart)) {
            return false;
        }
        if (descriptorStart == 0) {
            return true;
        }
        // the version directory must be the only one between the prefix and the descriptor
        return resourceName.startsWith(META_INF_VERSIONS_PREFIX)
                && resourceName.indexOf('/', META_INF_VERSIONS_PREFIX.length()) == descriptorStart - 1;
    }

    /**
     * Parses the {@code provides} directives of a {@code module-info.class}, grouping provider
     * class names by service interface. Modules may register service providers only in their
     * descriptor (without a {@code META-INF/services/} file), in which case they are found
     * by {@code ServiceLoader} when the application runs on the module path.
     */
    static void parseModuleInfoProviders(byte[] bytecode, Map<String, Set<String>> serviceProviders) {
        ClassFile.of().parse(bytecode).findAttribute(Attributes.module()).ifPresent(module -> {
            for (ModuleProvideInfo provide : module.provides()) {
                Set<String> names = serviceProviders.computeIfAbsent(
                        provide.provides().asInternalName().replace('/', '.'), k -> new HashSet<>());
                for (ClassEntry provider : provide.providesWith()) {
                    names.add(provider.asInternalName().replace('/', '.'));
                }
            }
        });
    }

    /**
     * Parses a {@code META-INF/services/} file, extracting provider class names
     * (stripping comments and whitespace) and grouping them by service interface.
     */
    private static void parseServiceFile(Path file, String relativePath,
            Map<String, Set<String>> serviceProviders) {
        String serviceInterface = relativePath.substring(META_INF_SERVICES.length());
        if (serviceInterface.isEmpty() || serviceInterface.contains("/")) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIdx = line.indexOf('#');
                if (commentIdx >= 0) {
                    line = line.substring(0, commentIdx);
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    serviceProviders.computeIfAbsent(serviceInterface, k -> new HashSet<>())
                            .add(line);
                }
            }
        } catch (IOException e) {
            log.debugf(e, "Failed to read service file: %s", file);
        }
    }

    /**
     * Parses a {@code META-INF/sisu/javax.inject.Named} file, extracting class names.
     * These classes are only included in the reachable set if a {@code ClassLoader.getResources()}
     * call for the sisu resource is detected during BFS.
     */
    private static void parseSisuNamedFile(Path file, Set<String> sisuNamedClasses) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIdx = line.indexOf('#');
                if (commentIdx >= 0) {
                    line = line.substring(0, commentIdx);
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    sisuNamedClasses.add(line);
                }
            }
        } catch (IOException e) {
            log.debugf(e, "Failed to read sisu named file: %s", file);
        }
    }

    /**
     * Releases all data structures. Must be called after stats computation is complete.
     */
    void releaseAnalysisData() {
        depBytecode.clear();
        classToDep.clear();
        appBytecode.clear();
        generatedBytecode.clear();
        serviceProviders.clear();
        serviceLoaderCalls.clear();
        sisuNamedClasses.clear();
        allKnownClasses.clear();
        depJarPaths.clear();
        appPaths.clear();
        transformedClassNames.clear();
        higherVersionBytecode.clear();
        roots.clear();
        conditionalRoots.clear();
        excludedArtifacts.clear();
        conditionalDeclaredClasses.clear();
        declaredClassesHolders.clear();
    }

    /**
     * Selects the module descriptors of an artifact that the JVM may read, following the same
     * multi-release rules as for classes: the highest version not newer than {@code appJavaVersion}
     * (a lower one is always overridden by it) and every newer version, since the application
     * may run on a newer JVM.
     * <p>
     * Newer versions are always selected, so their providers are collected as soon as they are
     * visited. The highest version not newer than {@code appJavaVersion} is only known once all the
     * entries have been visited, which is why its providers are collected by {@link #complete()}.
     */
    static class ModuleDescriptorSelector {

        private final int appJavaVersion;
        private final Map<String, Set<String>> serviceProviders;
        private int selectedVersion = -1;
        private byte[] selected;

        ModuleDescriptorSelector(int appJavaVersion, Map<String, Set<String>> serviceProviders) {
            this.appJavaVersion = appJavaVersion;
            this.serviceProviders = serviceProviders;
        }

        /**
         * Registers a module descriptor entry accepted by {@link #isModuleInfoEntry(String)}.
         * The descriptor is read right away, since the path is only guaranteed to be readable
         * while it's being visited.
         */
        void add(String entry, Path path) {
            final int version = multiReleaseVersion(entry);
            if (version > appJavaVersion) {
                final byte[] bytecode = readModuleDescriptor(path);
                if (bytecode != null) {
                    parseModuleInfoProviders(bytecode, serviceProviders);
                }
            } else if (version > selectedVersion) {
                final byte[] bytecode = readModuleDescriptor(path);
                if (bytecode != null) {
                    selectedVersion = version;
                    selected = bytecode;
                }
            }
        }

        /**
         * Collects the providers of the highest descriptor version not newer than {@code appJavaVersion}.
         * Must be called after all the entries of the artifact have been visited.
         */
        void complete() {
            if (selected != null) {
                parseModuleInfoProviders(selected, serviceProviders);
                selected = null;
            }
        }

        private static byte[] readModuleDescriptor(Path path) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                log.debugf(e, "Failed to read module descriptor: %s", path);
                return null;
            }
        }
    }

    /**
     * Lazy bytecode loader that reads from a {@link Path} on demand. The cache is typically
     * cleared immediately after use by {@link #readBytecode} to limit peak memory, so in
     * practice each read goes to disk. The path must remain valid (i.e. the owning
     * {@link OpenPathTree} must stay open) for the lifetime of this supplier.
     * Records the file size at construction for reporting.
     */
    static class BytecodeSupplier implements Supplier<byte[]> {
        private final Path path;
        final long size;

        BytecodeSupplier(Path path) {
            this.path = path;
            long s = 0;
            try {
                s = Files.size(path);
            } catch (IOException e) {
                // ignore, size is only used for reporting
            }
            this.size = s;
        }

        @Override
        public byte[] get() {
            try (InputStream is = Files.newInputStream(path)) {
                return is.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read bytecode " + path, e);
            }
        }
    }
}
