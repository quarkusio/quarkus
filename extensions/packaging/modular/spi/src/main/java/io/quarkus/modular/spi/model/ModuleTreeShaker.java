package io.quarkus.modular.spi.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.modular.spi.ClassPathUtils;
import io.quarkus.paths.PathTree;
import io.smallrye.modules.desc.Dependency;

/**
 * Prunes unreachable modules from an {@link AppModuleModel} using class-level reachability data
 * produced by the tree-shaking analysis in {@code core/deployment}.
 * <p>
 * The pruning operates in four phases:
 * <ol>
 * <li><b>Class-level liveness</b> — each module is checked against the reachable class set.
 * A module is <em>class-dead</em> if it contains {@code .class} entries but none of them are
 * reachable. Modules with no class entries (resource-only JARs) are treated as alive.
 * The application module is always protected from removal.</li>
 * <li><b>Graph reachability</b> — a BFS from root modules (application module + alive boot
 * modules) through the dependency graph. Modules not reachable from any root are
 * <em>graph-unreachable</em>. However, graph-unreachable modules that contain reachable classes
 * are <em>not</em> removed — they may be accessed via reflection, service loading, or from
 * automatic modules without explicit {@code requires} directives. A warning is logged for
 * such modules to help diagnose missing dependency declarations.</li>
 * <li><b>Dead module removal</b> — only class-dead modules are dropped.</li>
 * <li><b>Descriptor cleanup</b> — surviving modules have stale {@code requires} directives,
 * dead auto-dependency entries, and dead service implementations removed. The JDK module set
 * ({@code java.*}/{@code jdk.*}/{@code ibm.*}) is recomputed from surviving modules only,
 * which can significantly reduce the jlink image size.</li>
 * </ol>
 * <p>
 * This class is stateless beyond its constructor arguments and safe to use from a single thread.
 */
public final class ModuleTreeShaker {
    private static final Logger log = Logger.getLogger("io.quarkus.modular.spi");
    private static final String JAVA_SE = "java.se";

    private final AppModuleModel model;
    private final Set<String> reachableClassNames;
    private final Set<String> neededJdkModules;

    /**
     * Create a new tree shaker.
     *
     * @param model the application module model to prune (must not be {@code null})
     * @param reachableClassNames dot-separated names of all classes determined to be reachable
     *        by the class-level tree-shaking analysis (must not be {@code null})
     * @param referencedJdkPackages dot-separated JDK package names ({@code java.*}, {@code javax.*},
     *        {@code jdk.*}) referenced by reachable code; used to replace {@code requires java.se}
     *        with specific JDK module requirements (must not be {@code null})
     */
    public ModuleTreeShaker(AppModuleModel model, Set<String> reachableClassNames,
            Set<String> referencedJdkPackages) {
        this.model = model;
        this.reachableClassNames = reachableClassNames;
        this.neededJdkModules = computeNeededJdkModules(referencedJdkPackages);
    }

    /**
     * Run the module-level tree-shaking analysis and return the pruned model.
     * <p>
     * If no modules are dead, the original model instance is returned unchanged.
     *
     * @return a pruned {@link AppModuleModel}, or the original if nothing was pruned
     */
    public AppModuleModel shake() {
        Set<String> classDead = identifyDeadModules();
        Set<String> graphDead = findGraphUnreachableModules(classDead);
        if (!graphDead.isEmpty()) {
            logGraphDeadWithReachableClasses(graphDead);
        }
        return shake(classDead);
    }

    /**
     * Apply module-level tree-shaking with an explicitly provided set of dead module names.
     * <p>
     * This overload is package-private to allow unit tests to supply a known set of dead modules
     * without requiring real JAR content trees on disk.
     *
     * @param deadModules the names of modules to remove (may be empty)
     * @return a pruned {@link AppModuleModel}, or the original if {@code deadModules} is empty
     */
    AppModuleModel shake(Set<String> deadModules) {
        if (deadModules.isEmpty() && neededJdkModules.isEmpty()) {
            return model;
        }
        return rebuildModel(deadModules);
    }

    /**
     * Log a warning for modules that are graph-unreachable but contain reachable classes.
     * These modules are kept alive because their classes may be accessed via reflection,
     * service loading, or from automatic modules without explicit {@code requires} directives.
     *
     * @param graphDead the set of graph-unreachable module names with reachable classes
     */
    public static void logGraphDeadWithReachableClasses(Set<String> graphDead) {
        StringBuilder sb = new StringBuilder();
        sb.append("Module tree-shaking: ").append(graphDead.size())
                .append(" module(s) are graph-unreachable but contain reachable classes (keeping them):");
        graphDead.stream().sorted().forEach(name -> sb.append(System.lineSeparator()).append("  - ").append(name));
        log.warn(sb.toString());
    }

    /**
     * Scan all modules and classify each as alive or dead.
     * <p>
     * The application module is always classified as alive. All other modules —
     * including boot modules — are alive only if they have at least one reachable class.
     * Boot modules are expected to have reachable classes because the jlink extension
     * registers their entry points as tree-shake roots.
     * <p>
     * A module with no {@code .class} entries at all (a resource-only JAR) is treated
     * as alive — it may provide configuration files or other resources needed at runtime.
     *
     * @return the set of dead module names (may be empty, never {@code null})
     */
    private Set<String> identifyDeadModules() {
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        String appModuleName = model.appModuleInfo().name();

        Set<String> deadModules = new HashSet<>();
        log.debugf("Analyzing %d modules for class-level liveness (app=%s)", modulesByName.size(), appModuleName);
        for (ModuleInfo moduleInfo : modulesByName.values()) {
            String name = moduleInfo.name();
            if (name.equals(appModuleName)) {
                continue;
            }
            if (!hasReachableClass(moduleInfo)) {
                deadModules.add(name);
            }
        }
        return deadModules;
    }

    /**
     * Walk the module dependency graph from root modules and find modules that
     * are not reachable from any root. Root modules are the application module
     * and any boot modules that are not class-dead.
     * <p>
     * A module may have reachable classes yet still be graph-unreachable — for example
     * because it is accessed via reflection, service loading, or from an automatic module
     * without an explicit {@code requires} directive. Such modules are <em>not</em> treated
     * as dead; they are only logged as warnings to help diagnose missing dependency declarations.
     *
     * @param classDead modules already identified as dead by class-level analysis
     * @return the set of graph-unreachable module names that are not class-dead (may be empty)
     */
    Set<String> findGraphUnreachableModules(Set<String> classDead) {
        return findGraphUnreachableModules(model, classDead);
    }

    /**
     * Walk the module dependency graph from root modules and find modules that
     * are not reachable from any root. Root modules are the application module
     * and any boot modules that are not class-dead.
     * <p>
     * A module may have reachable classes yet still be graph-unreachable — for example
     * because it is accessed via reflection, service loading, or from an automatic module
     * without an explicit {@code requires} directive. Such modules are <em>not</em> treated
     * as dead; they are only logged as warnings to help diagnose missing dependency declarations.
     *
     * @param model the application module model
     * @param classDead modules already identified as dead by class-level analysis
     * @return the set of graph-unreachable module names that are not class-dead (may be empty)
     */
    public static Set<String> findGraphUnreachableModules(AppModuleModel model, Set<String> classDead) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(model.appModuleInfo().name());
        for (String boot : model.bootModules()) {
            if (!classDead.contains(boot)) {
                queue.add(boot);
            }
        }

        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (!visited.add(name) || classDead.contains(name)) {
                continue;
            }
            ModuleInfo mi = modulesByName.get(name);
            if (mi == null) {
                continue;
            }
            for (DependencyInfo dep : mi.dependencies()) {
                enqueueIfAppProvided(dep.moduleName(), classDead, visited, queue);
            }
            for (AutoDependencyGroup g : mi.autoDependencies()) {
                for (DependencyInfo dep : g.dependencies()) {
                    enqueueIfAppProvided(dep.moduleName(), classDead, visited, queue);
                }
            }
        }

        Set<String> graphDead = new HashSet<>();
        for (String name : modulesByName.keySet()) {
            if (!visited.contains(name) && !classDead.contains(name)) {
                graphDead.add(name);
            }
        }
        return graphDead;
    }

    /**
     * Add a module name to the BFS queue if it is an application-provided (non-JDK)
     * module that has not already been visited or classified as dead.
     *
     * @param name the module name to consider
     * @param dead the set of modules already classified as dead
     * @param visited the set of modules already visited by the BFS
     * @param queue the BFS queue to add to
     */
    private static void enqueueIfAppProvided(String name, Set<String> dead,
            Set<String> visited, Queue<String> queue) {
        if (!dead.contains(name) && !visited.contains(name) && !isJdkModule(name)) {
            queue.add(name);
        }
    }

    /**
     * Determine whether a module contains at least one reachable class.
     *
     * @param moduleInfo the module to inspect
     * @return {@code true} if the module should be kept alive
     */
    private boolean hasReachableClass(ModuleInfo moduleInfo) {
        return isModuleAlive(moduleInfo.resolvedArtifact().getContentTree(), moduleInfo.name(), reachableClassNames);
    }

    /**
     * Determine whether a module's content tree contains at least one reachable class.
     * <p>
     * Walks the content tree looking for {@code .class} entries (excluding
     * {@code module-info.class}). Returns {@code true} on the first reachable class found
     * (early exit). If the content tree is empty, returns {@code false} — the module has
     * no content to contribute. If the module contains no class files at all (a resource-only
     * JAR), returns {@code true} to keep it alive — it may provide configuration files,
     * native libraries, or other resources needed at runtime.
     *
     * @param contentTree the module's content tree
     * @param moduleName the module name (used for debug logging)
     * @param reachableClassNames dot-separated names of reachable classes
     * @return {@code true} if the module should be kept alive
     */
    public static boolean isModuleAlive(PathTree contentTree, String moduleName, Set<String> reachableClassNames) {
        if (contentTree.isEmpty()) {
            log.debugf("Module %s: content tree is empty, treating as dead", moduleName);
            return false;
        }
        boolean[] hasClasses = { false };
        boolean[] found = { false };
        contentTree.walk(visited -> {
            String rp = visited.getResourceName();
            if (ClassPathUtils.isClassEntry(rp)) {
                hasClasses[0] = true;
                String className = ClassPathUtils.resourcePathToClassName(rp);
                if (reachableClassNames.contains(className)) {
                    found[0] = true;
                    visited.stopWalking();
                }
            }
        });
        if (!hasClasses[0]) {
            log.debugf("Module %s: no class entries found, treating as alive (resource-only)", moduleName);
        } else if (!found[0]) {
            log.debugf("Module %s: has classes but none reachable, treating as dead", moduleName);
        }
        return !hasClasses[0] || found[0];
    }

    /**
     * Build a new {@link AppModuleModel} with dead modules removed and surviving modules cleaned.
     *
     * @param deadModules the set of module names to remove (must not be empty)
     * @return the rebuilt model
     */
    private AppModuleModel rebuildModel(Set<String> deadModules) {
        AppModuleModel.Builder amb = AppModuleModel.builder();
        Set<String> newJdkModules = new HashSet<>();
        String appModuleName = model.appModuleInfo().name();
        ModuleInfo cleanedAppModule = null;

        for (ModuleInfo moduleInfo : model.modulesByName().values()) {
            if (deadModules.contains(moduleInfo.name()) && !moduleInfo.name().equals(appModuleName)) {
                continue;
            }
            ModuleInfo cleaned = cleanModule(moduleInfo, deadModules);
            amb.moduleInfo(cleaned);
            collectJdkModules(cleaned, newJdkModules);
            if (moduleInfo.name().equals(appModuleName)) {
                cleanedAppModule = cleaned;
            }
        }

        amb.appModuleInfo(cleanedAppModule);
        newJdkModules.forEach(amb::jdkModuleUsed);
        Set<String> bootModules = survivingBootModules(deadModules);
        amb.bootModules(bootModules);

        int survivingModules = model.modulesByName().size() - deadModules.size();
        logSummary(deadModules, survivingModules - bootModules.size(), bootModules.size(), newJdkModules);
        return amb.build();
    }

    /**
     * Compute the set of boot modules that survived pruning.
     *
     * @param deadModules the set of dead module names
     * @return the surviving boot module names
     */
    private Set<String> survivingBootModules(Set<String> deadModules) {
        Set<String> surviving = new HashSet<>(model.bootModules());
        surviving.removeAll(deadModules);
        return surviving;
    }

    /**
     * Log a summary of module tree-shaking results at INFO level, with removed and
     * JDK module listings at DEBUG level.
     *
     * @param removedModules the set of removed module names
     * @param appModules the number of surviving application modules (excluding boot modules)
     * @param bootModules the number of surviving boot modules
     * @param jdkModules the recomputed set of JDK module names
     */
    private void logSummary(Set<String> removedModules, int appModules, int bootModules, Set<String> jdkModules) {
        log.infof("Module tree-shaking removed %d unreachable modules; image will contain %d app + %d JDK modules",
                removedModules.size(), appModules + bootModules, jdkModules.size());
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            if (!removedModules.isEmpty()) {
                sb.append("Removed modules:");
                removedModules.stream().sorted()
                        .forEach(name -> sb.append(System.lineSeparator()).append("  - ").append(name));
            }
            if (!jdkModules.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append(System.lineSeparator());
                }
                sb.append("JDK modules:");
                jdkModules.stream().sorted()
                        .forEach(name -> sb.append(System.lineSeparator()).append("  - ").append(name));
            }
            if (!sb.isEmpty()) {
                log.debug(sb.toString());
            }
        }
    }

    /**
     * Apply descriptor cleanup operations to a surviving module.
     * Removes stale dependencies pointing to dead modules, dead auto-dependency groups,
     * unreachable service implementations, and expands {@code requires java.se} into
     * specific JDK module requirements.
     * <p>
     * Package cleanup is not performed here because modules may contain non-class
     * resources (e.g. {@code .properties} files) that keep packages alive from jlink's
     * perspective. Correct package cleanup requires resource-aware analysis which is
     * beyond the scope of class-level tree-shaking.
     *
     * @param moduleInfo the module to clean up
     * @param deadModules the set of dead module names
     * @return the cleaned module, or the same instance if no changes were needed
     */
    private ModuleInfo cleanModule(ModuleInfo moduleInfo, Set<String> deadModules) {
        return cleanModule(moduleInfo, deadModules, reachableClassNames, neededJdkModules);
    }

    /**
     * Apply descriptor cleanup operations to a module, removing references to dead modules,
     * unreachable service implementations, and expanding {@code requires java.se}.
     *
     * @param moduleInfo the module to clean up
     * @param deadModules the set of dead module names
     * @param reachableClassNames dot-separated names of reachable classes
     * @param neededJdkModules the set of JDK module names needed by reachable code
     * @return the cleaned module, or the same instance if no changes were needed
     */
    public static ModuleInfo cleanModule(ModuleInfo moduleInfo, Set<String> deadModules,
            Set<String> reachableClassNames, Set<String> neededJdkModules) {
        moduleInfo = cleanDependencies(moduleInfo, deadModules);
        moduleInfo = cleanAutoDependencies(moduleInfo, deadModules);
        moduleInfo = cleanProvides(moduleInfo, reachableClassNames);
        moduleInfo = expandJavaSe(moduleInfo, neededJdkModules);
        return moduleInfo;
    }

    /**
     * Replace {@code requires java.se} with specific JDK module requirements based on
     * the JDK packages actually referenced by reachable code. This allows jlink to exclude
     * JDK modules that are not needed, significantly reducing the image size.
     * <p>
     * If the module does not have {@code requires java.se}, no change is made.
     *
     * @param moduleInfo the module to process
     * @return the module with {@code java.se} expanded, or the same instance if unchanged
     */
    private static ModuleInfo expandJavaSe(ModuleInfo moduleInfo, Set<String> neededJdkModules) {
        if (neededJdkModules.isEmpty()) {
            return moduleInfo;
        }
        List<DependencyInfo> deps = moduleInfo.dependencies();
        int javaseIdx = indexOfJavase(deps);
        if (javaseIdx < 0) {
            return moduleInfo;
        }

        DependencyInfo javaSeDep = deps.get(javaseIdx);
        Dependency.Modifier.Set mods = javaSeDep.modifiers().xor(Dependency.Modifier.SYNTHETIC);
        Set<String> existingDeps = getModuleNames(deps);

        List<DependencyInfo> expanded = new ArrayList<>(deps.size() + neededJdkModules.size());
        for (int i = 0; i < deps.size(); i++) {
            if (i == javaseIdx) {
                for (String jdkModule : neededJdkModules) {
                    if (!JAVA_SE.equals(jdkModule) && !existingDeps.contains(jdkModule)) {
                        expanded.add(new DependencyInfo(jdkModule, mods, Map.of()));
                    }
                }
            } else {
                expanded.add(deps.get(i));
            }
        }
        return moduleInfo.withDependencies(expanded);
    }

    private static Set<String> getModuleNames(List<DependencyInfo> deps) {
        Set<String> existingDeps = new HashSet<>(deps.size());
        for (DependencyInfo dep : deps) {
            existingDeps.add(dep.moduleName());
        }
        return existingDeps;
    }

    private static int indexOfJavase(List<DependencyInfo> deps) {
        int javaseIdx = -1;
        for (int i = 0; i < deps.size(); i++) {
            if (JAVA_SE.equals(deps.get(i).moduleName())) {
                javaseIdx = i;
                break;
            }
        }
        return javaseIdx;
    }

    /**
     * Remove {@code requires} directives that point to dead modules.
     * JDK modules ({@code java.*}, {@code jdk.*}, {@code ibm.*}) are never removed
     * since they are always available in the runtime.
     *
     * @param moduleInfo the module to clean
     * @param deadModules the set of dead module names
     * @return the module with stale requires removed, or the same instance if unchanged
     */
    private static ModuleInfo cleanDependencies(ModuleInfo moduleInfo, Set<String> deadModules) {
        List<DependencyInfo> deps = moduleInfo.dependencies();
        if (deps.isEmpty()) {
            return moduleInfo;
        }
        List<DependencyInfo> filtered = null;
        for (int i = 0; i < deps.size(); i++) {
            DependencyInfo dep = deps.get(i);
            if (deadModules.contains(dep.moduleName())) {
                if (filtered == null) {
                    filtered = new ArrayList<>(deps.size() - 1);
                    for (int j = 0; j < i; j++) {
                        filtered.add(deps.get(j));
                    }
                }
            } else if (filtered != null) {
                filtered.add(dep);
            }
        }
        return filtered != null ? moduleInfo.withDependencies(filtered) : moduleInfo;
    }

    /**
     * Remove auto-dependency groups whose host module is dead, and remove individual
     * entries within surviving groups that point to dead modules. If all entries in a
     * group are removed, the entire group is dropped.
     *
     * @param moduleInfo the module to clean
     * @param deadModules the set of dead module names
     * @return the module with stale auto-dependencies removed, or the same instance if unchanged
     */
    private static ModuleInfo cleanAutoDependencies(ModuleInfo moduleInfo, Set<String> deadModules) {
        List<AutoDependencyGroup> groups = moduleInfo.autoDependencies();
        if (groups.isEmpty()) {
            return moduleInfo;
        }
        List<AutoDependencyGroup> filtered = null;
        for (int i = 0; i < groups.size(); i++) {
            AutoDependencyGroup group = groups.get(i);
            AutoDependencyGroup cleanedGroup = cleanAutoGroup(group, deadModules);
            if (cleanedGroup != group) {
                if (filtered == null) {
                    filtered = new ArrayList<>(groups.size());
                    for (int j = 0; j < i; j++) {
                        filtered.add(groups.get(j));
                    }
                }
                if (cleanedGroup != null) {
                    filtered.add(cleanedGroup);
                }
            } else if (filtered != null) {
                filtered.add(group);
            }
        }
        return filtered != null ? moduleInfo.withAutoDependencies(filtered) : moduleInfo;
    }

    /**
     * Remove entries pointing to dead modules from a single auto-dependency group.
     *
     * @param group the group to filter
     * @param deadModules the set of dead module names
     * @return the filtered group, the same instance if unchanged, or {@code null} if all entries were removed
     */
    private static AutoDependencyGroup cleanAutoGroup(AutoDependencyGroup group, Set<String> deadModules) {
        List<DependencyInfo> deps = group.dependencies();
        List<DependencyInfo> filtered = null;
        for (int i = 0; i < deps.size(); i++) {
            DependencyInfo dep = deps.get(i);
            if (deadModules.contains(dep.moduleName())) {
                if (filtered == null) {
                    filtered = new ArrayList<>(deps.size() - 1);
                    for (int j = 0; j < i; j++) {
                        filtered.add(deps.get(j));
                    }
                }
            } else if (filtered != null) {
                filtered.add(dep);
            }
        }
        if (filtered == null) {
            return group;
        }
        return filtered.isEmpty() ? null : new AutoDependencyGroup(group.hostModuleName(), filtered);
    }

    /**
     * Remove service provider entries ({@code provides ... with ...}) whose implementation
     * classes are not in the reachable set. If all implementations for a given service interface
     * are removed, the entire {@code provides} entry is dropped.
     *
     * @param moduleInfo the module to clean
     * @return the module with dead service impls removed, or the same instance if unchanged
     */
    private static ModuleInfo cleanProvides(ModuleInfo moduleInfo, Set<String> reachableClassNames) {
        Map<String, List<String>> provides = moduleInfo.provides();
        if (provides.isEmpty()) {
            return moduleInfo;
        }
        Map<String, List<String>> filtered = null;
        for (var entry : provides.entrySet()) {
            List<String> aliveImpls = filterReachableImpls(entry.getValue(), reachableClassNames);
            if (aliveImpls != entry.getValue()) {
                if (filtered == null) {
                    filtered = new HashMap<>(provides);
                }
                if (aliveImpls.isEmpty()) {
                    filtered.remove(entry.getKey());
                } else {
                    filtered.put(entry.getKey(), aliveImpls);
                }
            }
        }
        return filtered != null ? moduleInfo.withProvides(filtered) : moduleInfo;
    }

    private static List<String> filterReachableImpls(List<String> impls, Set<String> reachableClassNames) {
        List<String> alive = null;
        for (int i = 0; i < impls.size(); i++) {
            String impl = impls.get(i);
            if (!reachableClassNames.contains(impl)) {
                if (alive == null) {
                    alive = new ArrayList<>(impls.size() - 1);
                    for (int j = 0; j < i; j++) {
                        alive.add(impls.get(j));
                    }
                }
            } else if (alive != null) {
                alive.add(impl);
            }
        }
        return alive != null ? alive : impls;
    }

    /**
     * Collect JDK module names ({@code java.*}, {@code jdk.*}, {@code ibm.*}) referenced
     * by a module's dependencies and auto-dependencies.
     *
     * @param moduleInfo the module to scan
     * @param jdkModules the set to add JDK module names to
     */
    public static void collectJdkModules(ModuleInfo moduleInfo, Set<String> jdkModules) {
        for (DependencyInfo dep : moduleInfo.dependencies()) {
            addIfJdk(dep.moduleName(), jdkModules);
        }
        for (AutoDependencyGroup group : moduleInfo.autoDependencies()) {
            for (DependencyInfo dep : group.dependencies()) {
                addIfJdk(dep.moduleName(), jdkModules);
            }
        }
    }

    /**
     * Add a module name to the JDK module set if it is a JDK module.
     *
     * @param moduleName the module name to check
     * @param jdkModules the set to add to
     */
    private static void addIfJdk(String moduleName, Set<String> jdkModules) {
        if (isJdkModule(moduleName)) {
            jdkModules.add(moduleName);
        }
    }

    /**
     * Compute the set of JDK module names actually needed by the application, based on
     * the JDK packages referenced from reachable code.
     * <p>
     * Iterating boot layer modules is sufficient because it covers all JDK modules;
     * application and library modules are not in the boot layer, so the lookup
     * naturally filters to JDK-only results. {@code java.base} is always included.
     * Packages that don't map to any system module (e.g. third-party {@code javax.*}
     * packages) are silently ignored.
     *
     * @param referencedJdkPackages the JDK package names referenced by reachable code
     * @return the set of JDK module names needed (never empty — always contains {@code java.base})
     */
    public static Set<String> computeNeededJdkModules(Set<String> referencedJdkPackages) {
        if (referencedJdkPackages.isEmpty()) {
            return Set.of();
        }
        Set<String> needed = new HashSet<>();
        needed.add("java.base");
        for (Module module : ModuleLayer.boot().modules()) {
            for (String pkg : module.getPackages()) {
                if (referencedJdkPackages.contains(pkg)) {
                    needed.add(module.getName());
                    break;
                }
            }
        }
        log.debugf("JDK modules needed by reachable code: %s", needed);
        return needed;
    }

    private static boolean isJdkModule(String name) {
        return name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("ibm.");
    }
}
