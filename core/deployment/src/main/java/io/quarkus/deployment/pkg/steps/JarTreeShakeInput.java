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
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeExcludedArtifactBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;

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
    private static final String META_INF_VERSIONS = "META-INF/versions";
    private static final String META_INF_SERVICES = "META-INF/services/";

    /** Configured tree-shake mode controlling analysis aggressiveness. */
    final TreeShakeConfig.TreeShakeMode treeShakeMode;
    /** Dot-separated class names that are unconditionally reachable (BFS starting points). */
    final Set<String> roots;
    /** Map from condition class to classes that become roots when the condition is reachable. */
    final Map<String, Set<String>> conditionalRoots;
    /** Classes listed in {@code META-INF/sisu/javax.inject.Named} files. */
    final Set<String> sisuNamedClasses;
    /** Bytecode suppliers for runtime dependency classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> depBytecode;
    /** Bytecode suppliers for application classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> appBytecode;
    /** Bytecode suppliers for build-time generated classes, keyed by dot-separated class name. */
    final Map<String, Supplier<byte[]>> generatedBytecode;
    /** Maps dot-separated dependency class names to their originating artifact. */
    final Map<String, ArtifactKey> classToDep;
    /** Service interface to provider class names parsed from {@code META-INF/services/} files. */
    final Map<String, Set<String>> serviceProviders;
    /** Service interface to consumer class names detected via {@code ServiceLoader.load()} calls. */
    final Map<String, Set<String>> serviceLoaderCalls;
    /** Union of dependency and generated class names, used for string-constant reference matching. */
    final Set<String> allKnownClasses;
    /** Java feature version detected from application bytecode (e.g. 17, 21). */
    final int appJavaVersion;
    /** Open content trees for dependencies, kept alive until {@link #close()}. */
    final Map<ArtifactKey, OpenPathTree> openTrees;
    /** Filesystem paths of resolved runtime dependency JARs. */
    final List<Path> depJarPaths;
    /** Filesystem paths of the application artifact. */
    final List<Path> appPaths;
    /** Class names whose bytecode was replaced by a build-time transformation. */
    final Set<String> transformedClassNames;
    /**
     * Bytecode from multi-release entries targeting Java versions newer than {@code appJavaVersion}.
     * Keyed by dot-separated class name. These are not used for the main reachability analysis
     * but their references must be traced after analysis to ensure classes they depend on
     * are also marked reachable.
     */
    final Map<String, List<Supplier<byte[]>>> higherVersionBytecode;
    /** Artifacts excluded from tree-shaking via {@link JarTreeShakeExcludedArtifactBuildItem}. */
    final Set<ArtifactKey> excludedArtifacts;

    /**
     * Creates a new tree-shake input with all collected data.
     * Prefer the {@link #collect} factory method, which assembles input from the application model
     * and build items. This constructor is used internally by {@code collect} and in tests.
     *
     * @param treeShakeMode configured tree-shake mode
     * @param roots unconditionally reachable class names (BFS starting points)
     * @param conditionalRoots condition class to classes that become roots when the condition is reachable
     * @param sisuNamedClasses classes from {@code META-INF/sisu/javax.inject.Named}
     * @param depBytecode bytecode suppliers for runtime dependency classes
     * @param appBytecode bytecode suppliers for application classes
     * @param generatedBytecode bytecode suppliers for build-time generated classes
     * @param classToDep dependency class name to originating artifact mapping
     * @param serviceProviders service interface to provider class names from {@code META-INF/services/}
     * @param serviceLoaderCalls service interface to consumer class names from {@code ServiceLoader.load()} calls
     * @param allKnownClasses union of dependency and generated class names
     * @param appJavaVersion Java feature version detected from application bytecode
     * @param openTrees open content trees for dependencies, closed by {@link #close()}
     * @param depJarPaths filesystem paths of resolved runtime dependency JARs
     * @param appPaths filesystem paths of the application artifact
     * @param transformedClassNames class names whose bytecode was replaced by a build-time transformation
     * @param higherVersionBytecode multi-release bytecode for Java versions above {@code appJavaVersion}
     * @param excludedArtifacts artifacts excluded from tree-shaking
     */
    JarTreeShakeInput(
            TreeShakeConfig.TreeShakeMode treeShakeMode,
            Set<String> roots,
            Map<String, Set<String>> conditionalRoots,
            Set<String> sisuNamedClasses,
            Map<String, Supplier<byte[]>> depBytecode,
            Map<String, Supplier<byte[]>> appBytecode,
            Map<String, Supplier<byte[]>> generatedBytecode,
            Map<String, ArtifactKey> classToDep,
            Map<String, Set<String>> serviceProviders,
            Map<String, Set<String>> serviceLoaderCalls,
            Set<String> allKnownClasses,
            int appJavaVersion,
            Map<ArtifactKey, OpenPathTree> openTrees,
            List<Path> depJarPaths,
            List<Path> appPaths,
            Set<String> transformedClassNames,
            Map<String, List<Supplier<byte[]>>> higherVersionBytecode,
            Set<ArtifactKey> excludedArtifacts) {
        this.treeShakeMode = treeShakeMode;
        this.roots = roots;
        this.conditionalRoots = conditionalRoots;
        this.sisuNamedClasses = sisuNamedClasses;
        this.depBytecode = depBytecode;
        this.appBytecode = appBytecode;
        this.generatedBytecode = generatedBytecode;
        this.classToDep = classToDep;
        this.serviceProviders = serviceProviders;
        this.serviceLoaderCalls = serviceLoaderCalls;
        this.allKnownClasses = allKnownClasses;
        this.appJavaVersion = appJavaVersion;
        this.openTrees = openTrees;
        this.depJarPaths = depJarPaths;
        this.appPaths = appPaths;
        this.transformedClassNames = transformedClassNames;
        this.higherVersionBytecode = higherVersionBytecode;
        this.excludedArtifacts = excludedArtifacts;
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
     */
    static JarTreeShakeInput collect(
            TreeShakeConfig.TreeShakeMode treeShakeMode,
            ApplicationModel appModel,
            List<GeneratedClassBuildItem> generatedClasses,
            TransformedClassesBuildItem transformedClasses,
            List<ReflectiveClassConditionBuildItem> reflectiveClassConditions,
            List<JarTreeShakeRootClassBuildItem> rootClasses,
            List<JarTreeShakeExcludedArtifactBuildItem> excludedArtifactItems) {

        final Set<String> roots = new HashSet<>();

        final Map<String, Supplier<byte[]>> generatedBytecode = getGeneratedClassesMap(generatedClasses);

        final Map<String, Supplier<byte[]>> depBytecode = new HashMap<>();
        final Map<String, ArtifactKey> classToDep = new HashMap<>();
        final Map<String, Integer> depBytecodeVersion = new HashMap<>();
        final Map<String, Set<String>> serviceProviders = new HashMap<>();
        final Set<String> sisuNamedClasses = new HashSet<>();
        final Map<String, Supplier<byte[]>> appBytecode = new HashMap<>();
        final Map<String, Set<String>> serviceLoaderCalls = new HashMap<>();
        final Map<ArtifactKey, OpenPathTree> openTrees = new HashMap<>();

        final int appJavaVersion = detectAppJavaVersion(appModel);

        final List<Path> depJarPaths = new ArrayList<>();
        final Map<String, List<Supplier<byte[]>>> higherVersionBytecode = new HashMap<>();

        collectRuntimeClasses(appModel, appJavaVersion, roots, depBytecode,
                classToDep, depBytecodeVersion, serviceProviders, sisuNamedClasses, openTrees,
                depJarPaths, higherVersionBytecode);

        final Set<String> transformedClassNames = new HashSet<>();
        collectTransformedClasses(transformedClasses, depBytecode, transformedClassNames);

        collectApplicationClasses(appModel, appBytecode, serviceProviders, openTrees);

        final List<Path> appPaths = new ArrayList<>();
        for (Path p : appModel.getAppArtifact().getResolvedPaths()) {
            appPaths.add(p);
        }

        addServiceProviderRoots(serviceProviders, classToDep, generatedBytecode, roots);

        final Set<String> allKnownClasses = collectAllKnownClasses(classToDep, generatedBytecode);
        addStringClassReferenceRoots(generatedBytecode, allKnownClasses, roots);

        for (JarTreeShakeRootClassBuildItem item : rootClasses) {
            roots.add(item.getClassName());
        }

        final Map<String, Set<String>> conditionalRoots = collectConditionalRoots(reflectiveClassConditions);

        return new JarTreeShakeInput(
                treeShakeMode,
                roots,
                conditionalRoots,
                sisuNamedClasses,
                depBytecode,
                appBytecode,
                generatedBytecode,
                classToDep,
                serviceProviders,
                serviceLoaderCalls,
                allKnownClasses,
                appJavaVersion,
                openTrees,
                depJarPaths,
                appPaths,
                transformedClassNames,
                higherVersionBytecode,
                getExcludedArtifacts(excludedArtifactItems));
    }

    /**
     * Walks all runtime classpath dependencies to collect bytecode (with multi-release resolution),
     * class-to-dependency mappings, service provider files, and sisu named components.
     * Classes from {@code quarkus-bootstrap-runner} are added as roots.
     */
    private static void collectRuntimeClasses(
            ApplicationModel appModel,
            int appJavaVersion,
            Set<String> roots,
            Map<String, Supplier<byte[]>> depBytecode,
            Map<String, ArtifactKey> classToDep,
            Map<String, Integer> depBytecodeVersion,
            Map<String, Set<String>> serviceProviders,
            Set<String> sisuNamedClasses,
            Map<ArtifactKey, OpenPathTree> openTrees,
            List<Path> depJarPaths,
            Map<String, List<Supplier<byte[]>>> higherVersionBytecode) {

        for (ResolvedDependency dep : appModel.getDependencies(DependencyFlags.RUNTIME_CP)) {
            final boolean addClassesAsRoots = "quarkus-bootstrap-runner".equals(dep.getArtifactId());

            for (Path p : dep.getResolvedPaths()) {
                depJarPaths.add(p);
            }
            openPathTree(dep, openTrees).walkRaw(visit -> {
                String entry = visit.getResourceName();
                if (isClassEntry(entry)) {
                    processRuntimeClassEntry(entry, visit.getPath(), dep, appJavaVersion, addClassesAsRoots,
                            roots, depBytecode, classToDep, depBytecodeVersion,
                            higherVersionBytecode);
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
        }
    }

    /**
     * Processes a single class entry from a runtime dependency JAR. Handles multi-release
     * resolution (keeping the highest version &le; {@code appJavaVersion}), records the
     * bytecode supplier and class-to-dependency mapping, and optionally adds the class as a root.
     */
    private static void processRuntimeClassEntry(
            String entry,
            Path path,
            ResolvedDependency dep,
            int appJavaVersion,
            boolean addClassesAsRoots,
            Set<String> roots,
            Map<String, Supplier<byte[]>> depBytecode,
            Map<String, ArtifactKey> classToDep,
            Map<String, Integer> depBytecodeVersion,
            Map<String, List<Supplier<byte[]>>> higherVersionBytecode) {

        String className;
        int classJavaVersion = 0;

        if (entry.startsWith(META_INF_VERSIONS)) {
            int resolved = resolveMultiReleaseVersion(entry, appJavaVersion);
            if (resolved < 0) {
                // Multi-release entry for a Java version newer than appJavaVersion.
                // Collect its bytecode so references can be traced after the main analysis
                // for classes that are reachable. Also register in classToDep so the class
                // is part of allKnownClasses (needed for reference matching).
                int javaVersionSeparator = entry.indexOf('/', META_INF_VERSIONS.length() + 1);
                if (javaVersionSeparator > 0) {
                    String cn = classNameOf(entry, javaVersionSeparator + 1);
                    higherVersionBytecode.computeIfAbsent(cn, k -> new ArrayList<>())
                            .add(new BytecodeSupplier(path));
                    classToDep.putIfAbsent(cn, dep.getKey());
                }
                return;
            }
            classJavaVersion = resolved;
            int javaVersionSeparator = entry.indexOf('/', META_INF_VERSIONS.length() + 1);
            className = classNameOf(entry, javaVersionSeparator + 1);
        } else {
            className = classNameOf(entry);
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
     * Resolves a multi-release version entry, returning the Java version number
     * if it should be included, or -1 if it should be skipped.
     */
    private static int resolveMultiReleaseVersion(String entry, int appJavaVersion) {
        if (entry.length() == META_INF_VERSIONS.length() || entry.charAt(META_INF_VERSIONS.length()) != '/') {
            return -1;
        }
        final int javaVersionSeparator = entry.indexOf('/', META_INF_VERSIONS.length() + 1);
        if (javaVersionSeparator == -1) {
            return -1;
        }
        try {
            int classJavaVersion = Integer.parseInt(
                    entry.substring(META_INF_VERSIONS.length() + 1, javaVersionSeparator));
            if (classJavaVersion > appJavaVersion) {
                return -1;
            }
            return classJavaVersion;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Overrides dependency bytecode with transformed bytecode where available.
     * Transformations can add interfaces, annotations, or other references
     * that aren't in the original bytecode (e.g. NettySharable marker interface).
     */
    private static void collectTransformedClasses(
            TransformedClassesBuildItem transformedClasses,
            Map<String, Supplier<byte[]>> depBytecode,
            Set<String> transformedClassNames) {
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
    private static void collectApplicationClasses(
            ApplicationModel appModel,
            Map<String, Supplier<byte[]>> appBytecode,
            Map<String, Set<String>> serviceProviders,
            Map<ArtifactKey, OpenPathTree> openTrees) {
        openPathTree(appModel.getAppArtifact(), openTrees).walk(visit -> {
            String entry = visit.getResourceName();
            if (isClassEntry(entry)) {
                appBytecode.put(classNameOf(entry), new BytecodeSupplier(visit.getPath()));
                return;
            }
            if (entry.startsWith(META_INF_SERVICES) && !entry.endsWith("/")) {
                parseServiceFile(visit.getPath(), entry, serviceProviders);
            }
        });
    }

    /**
     * Add service providers for JDK service interfaces as roots, since JDK code
     * isn't analyzed and its ServiceLoader.load() calls can't be traced.
     * Non-JDK service providers are discovered through ServiceLoader.load() call
     * tracing in reachable dependency/app code.
     */
    private static void addServiceProviderRoots(
            Map<String, Set<String>> serviceProviders,
            Map<String, ArtifactKey> classToDep,
            Map<String, Supplier<byte[]>> generatedBytecode,
            Set<String> roots) {
        for (Map.Entry<String, Set<String>> entry : serviceProviders.entrySet()) {
            String serviceInterface = entry.getKey();
            if (!classToDep.containsKey(serviceInterface) && !generatedBytecode.containsKey(serviceInterface)) {
                roots.addAll(entry.getValue());
            }
        }
    }

    /**
     * Returns the union of dependency and generated class names, used for string-constant
     * class reference matching.
     */
    private static Set<String> collectAllKnownClasses(
            Map<String, ArtifactKey> classToDep,
            Map<String, Supplier<byte[]>> generatedBytecode) {
        final Set<String> allKnownClasses = new HashSet<>(classToDep.keySet());
        allKnownClasses.addAll(generatedBytecode.keySet());
        return allKnownClasses;
    }

    /**
     * Scan generated classes for LDC string constants that match known dependency class names.
     * Generated recorder bytecode often passes class names as strings to methods like
     * factory(className) that eventually call Class.forName() at runtime.
     */
    private static void addStringClassReferenceRoots(
            Map<String, Supplier<byte[]>> generatedBytecode,
            Set<String> allKnownClasses,
            Set<String> roots) {
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
     * Builds a map from condition type to the set of class names that should become roots
     * when that condition type is reachable. Used by {@link JarTreeShaker#evaluateConditionalRoots}
     * for fixed-point evaluation.
     */
    private static Map<String, Set<String>> collectConditionalRoots(
            List<ReflectiveClassConditionBuildItem> reflectiveClassConditions) {
        Map<String, Set<String>> conditionalRoots = new HashMap<>();
        for (ReflectiveClassConditionBuildItem item : reflectiveClassConditions) {
            conditionalRoots
                    .computeIfAbsent(item.getTypeReachable(), k -> new HashSet<>())
                    .add(item.getClassName());
        }
        return conditionalRoots;
    }

    /**
     * Converts {@link GeneratedClassBuildItem} list to a class-name-to-bytecode-supplier map,
     * translating binary names (with {@code /}) to dot-separated class names.
     */
    private static Map<String, Supplier<byte[]>> getGeneratedClassesMap(List<GeneratedClassBuildItem> generatedClasses) {
        final Map<String, Supplier<byte[]>> generatedBytecode = new HashMap<>(generatedClasses.size());
        for (GeneratedClassBuildItem gen : generatedClasses) {
            generatedBytecode.put(gen.binaryName().replace('/', '.'), gen::getClassData);
        }
        return generatedBytecode;
    }

    private static Set<ArtifactKey> getExcludedArtifacts(
            List<JarTreeShakeExcludedArtifactBuildItem> excludedArtifacts) {
        Set<ArtifactKey> excludedKeys = new HashSet<>(excludedArtifacts.size());
        for (JarTreeShakeExcludedArtifactBuildItem item : excludedArtifacts) {
            excludedKeys.add(item.getArtifactKey());
        }
        return excludedKeys;
    }

    // ---- Supporting methods ----

    /**
     * Opens a dependency's content tree and registers it for cleanup on {@link #close()}.
     */
    private static OpenPathTree openPathTree(ResolvedDependency dep, Map<ArtifactKey, OpenPathTree> openTrees) {
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

    /**
     * Returns {@code true} if the resource name represents a regular class file,
     * excluding {@code module-info.class} and {@code package-info.class}.
     */
    static boolean isClassEntry(String resourceName) {
        return resourceName.endsWith(".class")
                && !resourceName.equals("module-info.class")
                && !resourceName.endsWith("package-info.class");
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
