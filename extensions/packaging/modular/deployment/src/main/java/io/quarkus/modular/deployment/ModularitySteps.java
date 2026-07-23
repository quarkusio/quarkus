package io.quarkus.modular.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.constant.ClassDesc;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.modular.spi.items.AddDependencyBuildItem;
import io.quarkus.modular.spi.items.ApplicationModuleInfoBuildItem;
import io.quarkus.modular.spi.items.BootModulePathBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.AutoDependencyGroup;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.quarkus.modular.spi.model.ModuleTreeShaker;
import io.quarkus.paths.ManifestAttributes;
import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.AnnotationValue;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.attribute.ModuleMainClassAttribute;
import io.smallrye.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import io.smallrye.classfile.constantpool.ClassEntry;
import io.smallrye.classfile.constantpool.Utf8Entry;
import io.smallrye.classfile.extras.reflect.AccessFlag;
import io.smallrye.common.resource.MemoryResource;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency.Modifier;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

public final class ModularitySteps {
    private static final Logger log = Logger.getLogger("io.quarkus.modular.deployment");

    public ModularitySteps() {
    }

    @BuildStep
    public List<BootModulePathBuildItem> standardBootItems() {
        return List.of(
                new BootModulePathBuildItem("io.quarkus.bootstrap.runner"),
                new BootModulePathBuildItem("org.jboss.logmanager"),
                new BootModulePathBuildItem("org.jboss.logmanager.slf4j"),
                new BootModulePathBuildItem("io.smallrye.modules"));
    }

    @BuildStep
    public List<AddDependencyBuildItem> standardAddedDependencies(
            CurateOutcomeBuildItem curateOutcome) {
        // TODO: migrate these to their relevant extensions
        return List.of(
                new AddDependencyBuildItem("org.eclipse.microprofile.config", "io.smallrye.config",
                        Modifier.Set.of(Modifier.SERVICES)),
                // todo: this one must be READ and LINKED because an ArC synthetic bean requires it
                new AddDependencyBuildItem("io.netty.transport", "io.quarkus.netty",
                        Modifier.Set.of(Modifier.SERVICES, Modifier.READ, Modifier.LINKED)),
                new AddDependencyBuildItem("jakarta.ws.rs", "io.quarkus.resteasy.reactive.common",
                        Modifier.Set.of(Modifier.SERVICES, Modifier.OPTIONAL)),
                new AddDependencyBuildItem("org.slf4j", "org.jboss.logmanager.slf4j", Modifier.Set.of(Modifier.SERVICES)));
    }

    @BuildStep
    public ApplicationModuleInfoBuildItem buildModularityModel(
            CurateOutcomeBuildItem curateOutcome,
            MainClassBuildItem mainClassBuildItem,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<GeneratedServiceProviderBuildItem> generatedServices,
            TransformedClassesBuildItem transformedClasses,
            List<ModuleOpenBuildItem> opens,
            // TODO: List<ModuleExportBuildItem> exports,
            List<ModuleEnableNativeAccessBuildItem> nativeAccesses,
            List<AddDependencyBuildItem> extraDeps,
            List<BootModulePathBuildItem> bootPathItems,
            JarTreeShakeBuildItem treeShakeResult) {

        /* @formatter:off
         * Build the modular application model. This is done in a few stages.
         *
         * • Find all of the artifacts present in the application
         * • Collect and index the build items relating to modularity (add opens/exports, native access, etc.)
         * • (temporary) create an index of which packages belong to which modules
         *   ◦ Note: this is temporary because it prohibits packages from existing in one more module.
         *     See #44657 for more information on why this is temporarily necessary.
         * • (temporary) add ArC dependencies
         *   ◦ Note: this is temporary as we would want to move this to the ArC extension.
         *     However we presently do not have a strategy for this. See #52933.
         * • Create the module set from the set of artifacts
         *   ◦ This may entail traversing dependency sets for automatic modules
         *   ◦ The output of this stage is a `io.quarkus.modular.spi.model.ModuleInfo` for each module
         * • Calculate the set of boot modules
         *   ◦ This is derived from the dependency graph of each BootModulePathBuildItem value
         * • Assemble the modules into the final ApplicationModuleInfoBuildItem
         * @formatter:on
         */

        // Find all artifacts

        ApplicationModel model = curateOutcome.getApplicationModel();
        // This is the single application artifact/module.
        ResolvedDependency appArtifact = model.getAppArtifact();

        // tabulate all generated and transformed resources, by package, so we can assign them to modules
        Map<String, Map<String, Resource>> generatedByPackageAndPath = new HashMap<>();
        // the set of packages containing ArC generated classes
        // todo: replace this with some build item that adds the dependency
        Set<String> arcGeneratedPackages = new HashSet<>();
        Set<String> arcRuntimeGeneratedPackages = new HashSet<>();
        Set<String> extraAppModuleDepPackages = new HashSet<>();

        // Collect the initial set of modules that must be on the boot path.
        Set<String> bootPathModules = bootPathItems.stream().map(BootModulePathBuildItem::moduleName)
                .collect(Collectors.toSet());

        // Get all known artifacts with their module names and build an index.
        Map<String, ResolvedDependency> knownNamedModules = model.getDependencies().stream()
                .filter(d -> d.isFlagSet(DependencyFlags.RUNTIME_CP))
                .filter(d -> !d.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION))
                .map(d -> Map.entry(d.getModuleName(), d))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Collect all added dependencies and merge them into a flat map.
        // In particular, we collapse duplicates by combining modifier sets union-wise.
        // The mapping here is: from module -> to module -> with modifier(s)
        Map<String, Map<String, Modifier.Set>> extraDepsMap = extraDeps.stream()
                .collect(Collectors.groupingBy(
                        AddDependencyBuildItem::module,
                        Collectors.groupingBy(
                                AddDependencyBuildItem::targetModule,
                                Collectors.mapping(AddDependencyBuildItem::modifiers,
                                        Collectors.reducing(Modifier.Set.of(), Modifier.Set::withAll)))));

        // Collect all add-opens and merge them into a flat map.
        // We collapse duplicates by taking the union of the opened packages.
        // Note that add-opens will take precedence over add-exports.
        // opening module -> opened module -> package set
        Map<String, Map<String, Set<String>>> addOpensByModule = opens.stream()
                .collect(Collectors.groupingBy(
                        ModuleOpenBuildItem::openingModuleName,
                        Collectors.groupingBy(
                                ModuleOpenBuildItem::openedModuleName,
                                Collectors.flatMapping(mobi -> mobi.packageNames().stream(),
                                        Collectors.toSet()))));
        // Collect the set of modules which require native access.
        Set<String> nativeAccessNames = nativeAccesses.stream()
                .map(ModuleEnableNativeAccessBuildItem::moduleName)
                .collect(Collectors.toUnmodifiableSet());

        // Here's the temporary modules-by-package index used until #44657 can be resolved.
        Map<String, String> modulesByPackageTemporary = new HashMap<>();

        // Analyze generated classes for ArC (#52933) and for #44657.
        for (GeneratedClassBuildItem gcbi : generatedClasses) {
            String pn = gcbi.packageName();
            String bn = gcbi.binaryName();
            String name = gcbi.internalName() + ".class";
            // todo: change GeneratedClassBuildItem to use Resource instead of byte[]
            generatedByPackageAndPath.computeIfAbsent(pn, ModularitySteps::newMap)
                    .put(name, new MemoryResource(name, gcbi.getClassData()));
            // TODO: We need a better way to detect ArC dependencies using AddDependencyBuildItem
            if (bn.endsWith("_Synthetic_Bean") || bn.endsWith("_Subclass")) {
                arcRuntimeGeneratedPackages.add(pn);
            }
            if (bn.endsWith("_Bean") || bn.endsWith("_ArcAnnotationLiteral")) {
                arcGeneratedPackages.add(pn);
            }
            if (bn.contains("_ComponentsProvider")) {
                // todo: temporarily scrape the constant pool to add extra dependencies to the app module
                ClassModel cm = ClassFile.of().parse(gcbi.getClassData());
                cm.constantPool().forEach(pe -> {
                    if (pe instanceof ClassEntry ce) {
                        ClassDesc desc = ce.asSymbol();
                        if (desc.isClassOrInterface()) {
                            extraAppModuleDepPackages.add(desc.packageName());
                        }
                    }
                });
            }
        }

        // impl package -> service name -> impl name
        Map<String, Map<String, List<String>>> extraServicesByPackage = new HashMap<>();
        for (GeneratedServiceProviderBuildItem item : generatedServices) {
            String apiName = item.getServiceInterfaceName();
            String impl = item.getImplementationClassName();
            int lastDot = impl.lastIndexOf('.');
            if (lastDot != -1) {
                String pkgName = impl.substring(0, lastDot);
                extraServicesByPackage.computeIfAbsent(pkgName, ModularitySteps::newMap)
                        .computeIfAbsent(apiName, ModularitySteps::newList)
                        .add(impl);
            }
        }
        // Gather up the set of generated resources for indexing (#44657).
        for (GeneratedResourceBuildItem rsrc : generatedResources) {
            String name = rsrc.getName();
            String pn;
            int idx = name.lastIndexOf('/');
            if (idx == -1) {
                pn = "";
            } else {
                pn = name.substring(0, idx).replace('/', '.');
            }
            // todo: change GeneratedResourceBuildItem to use Resource instead of byte[]
            generatedByPackageAndPath.computeIfAbsent(pn, ModularitySteps::newMap)
                    .put(name, new MemoryResource(name, rsrc.getData()));
        }

        // Scan transformed classes to make sure they don't overlap with a generated class and index it (#44657).
        for (Set<TransformedClassesBuildItem.TransformedClass> set : transformedClasses.getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass tc : set) {
                String bn = tc.getClassName();
                String fileName = tc.getFileName();
                String pn;
                if (bn == null) {
                    // a removed resource rather than a transformed class (see TransformedClassesBuildItem);
                    // derive the package from the resource path, as is done for generated resources above
                    int idx = fileName.lastIndexOf('/');
                    if (idx == -1) {
                        pn = "";
                    } else {
                        pn = fileName.substring(0, idx).replace('/', '.');
                    }
                } else {
                    int idx = bn.lastIndexOf('.');
                    if (idx == -1) {
                        pn = "";
                    } else {
                        pn = bn.substring(0, idx);
                    }
                }
                // replace any existing entry
                byte[] data = tc.getData();
                if (data == null) {
                    // TODO
                    if (generatedByPackageAndPath.containsKey(pn)) {
                        generatedByPackageAndPath.get(pn).remove(fileName);
                    }
                } else {
                    generatedByPackageAndPath.computeIfAbsent(pn, ModularitySteps::newMap)
                            .put(fileName, new MemoryResource(fileName, data));
                }
            }
        }

        // now start creating the module set

        Set<String> usedJdkModuleNames = new HashSet<>();

        // This is just an index of each artifact by groupId+artifactId.
        Map<ArtifactKey, ResolvedDependency> allDependenciesByKey = knownNamedModules.values().stream().map(
                d -> Map.entry(keyOf(d), d)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // This will be our collection of all modules in the application.
        Map<String, ModuleInfo> modulesByName = new HashMap<>();

        // map of all `module-info.class` models
        Map<ResolvedDependency, ClassModel> moduleInfoClassModels = knownNamedModules.values().stream()
                .map(d -> {
                    PathTree contentTree = d.getContentTree();
                    ClassModel cm = findModuleInfoClass(contentTree, "module-info.class");
                    if (cm == null) {
                        // slf4j...
                        cm = findModuleInfoClass(contentTree, "META-INF/versions/9/module-info.class");
                    }
                    return cm == null ? null : Map.entry(d, cm);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // The application's module name
        final String appModuleName = appArtifact.getModuleName();

        // ArC must depend on the application module (for service loading)
        // todo: ArC should use AddDependencyBuildItem for this.
        extraDepsMap.computeIfAbsent("io.quarkus.arc", ModularitySteps::newMap)
                .put(appModuleName, Modifier.Set.of(Modifier.SERVICES));

        // Vert.x core needs to link against `io.quarkus.vertx`.
        // todo: The Vert.x extension should use AddDependencyBuildItem for this.
        extraDepsMap.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                .put("io.quarkus.vertx", Modifier.Set.of(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC));

        // Now go through the process to build a (Quarkus) module descriptor for each artifact.
        for (ResolvedDependency dependency : knownNamedModules.values()) {
            if (!dependency.isRuntimeCp()) {
                continue;
            }
            String moduleName = dependency.getModuleName();
            if (moduleName.equals(appModuleName)) {
                // we do app module at the end
                continue;
            }
            // Get the module info.
            // todo: we're doing a lot of extra transformations here that will eventually move out.
            final ModuleInfo depModule = getModuleInfo(dependency, generatedByPackageAndPath, moduleInfoClassModels,
                    allDependenciesByKey, Map.of(), knownNamedModules, mi -> {
                        // Transformations for a given module.
                        // These flags ensure we don't add dependencies twice (see #52933).
                        boolean arcAdded = false;
                        boolean arcRuntimeAdded = false;
                        // Add the native-access flag if a build item says to do so.
                        if (nativeAccessNames.contains(moduleName)) {
                            mi = mi.withModifier(ModuleDescriptor.Modifier.NATIVE_ACCESS);
                        }
                        for (String pkg : mi.packages().keySet()) {
                            // Add service providers found in build items.
                            if (extraServicesByPackage.containsKey(pkg)) {
                                mi = mi.withMoreServices(extraServicesByPackage.remove(pkg));
                            }
                            // Handle the ArC stuff (#52933).
                            // TODO: use build items to do this more efficiently up front
                            if (!arcAdded && arcGeneratedPackages.contains(pkg)) {
                                if (!mi.name().equals("io.quarkus.arc")) {
                                    mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                            "io.quarkus.arc",
                                            Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                            Map.of())));
                                }
                                if (!mi.name().equals("jakarta.cdi")) {
                                    mi = mi.withMoreDependencies(List.of(new DependencyInfo("jakarta.cdi",
                                            Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                            Map.of())));
                                }
                                arcAdded = true;
                            }
                            if (!arcRuntimeAdded && arcRuntimeGeneratedPackages.contains(pkg)
                                    && !mi.name().equals("io.quarkus.arc.runtime")) {
                                mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                        "io.quarkus.arc.runtime",
                                        Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                        Map.of())));
                                arcRuntimeAdded = true;
                            }
                        }
                        // TODO: Temporary fixups; remove when we can.
                        switch (moduleName) {
                            // todo: needs resteasy release
                            case "org.jboss.resteasy.core.spi" -> {
                                mi = mi.withMorePackages(Map.of("org.jboss.resteasy.resteasy_jaxrs.i18n", PackageInfo.of(
                                        PackageAccess.PRIVATE, Set.of(), Set.of("org.jboss.logging"))));
                            }
                        }
                        // Add extra dependencies from build items.
                        mi = mi.withMoreDependencies(extraDepsMap.getOrDefault(mi.name(), Map.of())
                                .entrySet()
                                .stream()
                                .map((e) -> new DependencyInfo(e.getKey(), e.getValue(), Map.of()))
                                .toList());
                        // Add extra opens from build items.
                        Map<String, Set<String>> obi = addOpensByModule.getOrDefault(moduleName, Map.of());
                        if (!obi.isEmpty()) {
                            mi = mi.withMoreDependencies(obi
                                    .entrySet()
                                    .stream()
                                    .map((e) -> new DependencyInfo(
                                            e.getKey(),
                                            Modifier.Set.of(Modifier.READ, Modifier.OPTIONAL),
                                            e.getValue().stream().collect(
                                                    Collectors.toMap(Function.identity(), ignored -> PackageAccess.OPEN))))
                                    .toList());
                        }
                        // tabulate any used JDK modules.
                        mi.dependencies().stream()
                                .map(DependencyInfo::moduleName)
                                .filter(n -> n.startsWith("java.") || n.startsWith("jdk.") || n.startsWith("ibm."))
                                .forEach(usedJdkModuleNames::add);
                        return mi;
                    });
            // Add the module to the index.
            if (modulesByName.containsKey(moduleName)) {
                ModuleInfo existing = modulesByName.get(moduleName);
                if (existing.equals(depModule)) {
                    // no harm; it's just in there twice for some reason
                    continue;
                }
                throw new IllegalStateException("Module '" + moduleName + "' has been defined twice, in: " +
                        depModule.resolvedArtifact() + " and " + existing.resolvedArtifact());
            }
            modulesByName.put(moduleName, depModule);
            // Warn about any split packages (temporary until #44657; then we don't care as much about it).
            depModule.packages().keySet().forEach(pn -> {
                String existing = modulesByPackageTemporary.putIfAbsent(pn, moduleName);
                if (existing != null && !existing.equals(moduleName)) {
                    log.warnf("Package %s is split, in %s and %s", pn, existing, moduleName);
                }
            });
        }

        // Compute the set of extra app module dependencies from ArC component providers.
        // todo: Remove once #52933 is sorted out.
        for (String pn : extraAppModuleDepPackages) {
            String moduleName = modulesByPackageTemporary.get(pn);
            if (moduleName != null) {
                extraDepsMap.computeIfAbsent(appModuleName, ModularitySteps::newMap)
                        .compute(moduleName,
                                (pn1, mods) -> mods == null
                                        ? Modifier.Set.of(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC)
                                        : mods.withAll(Modifier.READ, Modifier.LINKED));
            }
        }

        // Assemble the initial extra packages map for the application module.
        // todo: once #44657 is resolved, we should be able to get this from the build items themselves.
        Map<String, PackageInfo> initialPackages = new HashMap<>(Map.of(
                "", PackageInfo.PRIVATE,
                "io.quarkus.runner", PackageInfo.EXPORTED,
                "io.quarkus.runtime.generated", PackageInfo.EXPORTED,
                "io.quarkus.deployment.steps", PackageInfo.EXPORTED,
                "io.quarkus.rest.runtime", PackageInfo.EXPORTED,
                "io.quarkus.arc.setup", PackageInfo.EXPORTED,
                "io.quarkus.arc.generator", PackageInfo.EXPORTED,
                "io.quarkus.runner.recorded", PackageInfo.EXPORTED));

        // claim all unclaimed generated classes and resources (#44657)
        generatedByPackageAndPath.keySet().forEach(pn -> initialPackages.putIfAbsent(pn, PackageInfo.PRIVATE));

        // Get the descriptor information for the application module.
        // todo: we do a few extra transformations for ArC due to #52933.
        final ModuleInfo appModule = getModuleInfo(appArtifact, generatedByPackageAndPath, moduleInfoClassModels,
                allDependenciesByKey, initialPackages,
                knownNamedModules, mi -> {
                    mi = mi.withMainClass(mainClassBuildItem.getClassName());
                    // add services and dependencies to app module
                    boolean arcAdded = false;
                    boolean arcRuntimeAdded = false;
                    for (String pkg : mi.packages().keySet()) {
                        if (extraServicesByPackage.containsKey(pkg)) {
                            mi = mi.withMoreServices(extraServicesByPackage.get(pkg));
                        }
                        // TODO: use build items to do this more efficiently up front
                        if (!arcAdded && arcGeneratedPackages.contains(pkg)) {
                            mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                    "io.quarkus.arc", Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                    Map.of())));
                            arcAdded = true;
                        }
                        if (!arcRuntimeAdded && arcRuntimeGeneratedPackages.contains(pkg)) {
                            mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                    "io.quarkus.arc.runtime",
                                    Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                    Map.of())));
                            arcRuntimeAdded = true;
                        }
                    }
                    // now add all extra dependencies (from build items)
                    mi = mi.withMoreDependencies(extraDepsMap.getOrDefault(mi.name(), Map.of())
                            .entrySet()
                            .stream()
                            .map((e) -> new DependencyInfo(e.getKey(), e.getValue(), Map.of()))
                            .toList());
                    // tabulate any used JDK modules.
                    mi.dependencies().stream()
                            .map(DependencyInfo::moduleName)
                            .filter(n -> n.startsWith("java.") || n.startsWith("jdk.") || n.startsWith("ibm."))
                            .forEach(usedJdkModuleNames::add);
                    return mi;
                });

        // Register the app module with the others.
        modulesByName.put(appModuleName, appModule);

        // -----------------------------------
        // at this point, appModule is frozen!
        // -----------------------------------

        // determine the set of boot-path modules
        // todo: this is packaging-dependent!
        Set<ModuleInfo> bootModuleSet = new HashSet<>();
        HashSet<String> visited = new HashSet<>();
        for (ModuleInfo moduleInfo : modulesByName.values()) {
            if (bootPathModules.contains(moduleInfo.name())) {
                computeBootPath(moduleInfo, modulesByName, visited, bootModuleSet);
            }
        }
        // Build and return the final modular model.
        AppModuleModel.Builder amb = AppModuleModel.builder();
        amb.appModuleInfo(appModule);
        usedJdkModuleNames.forEach(amb::jdkModuleUsed);
        bootModuleSet.forEach(m -> amb.bootModule(m.name()));
        modulesByName.values().forEach(amb::moduleInfo);
        AppModuleModel appModuleModel = amb.build();

        if (treeShakeResult.isClassesShaken()) {
            appModuleModel = new ModuleTreeShaker(appModuleModel, treeShakeResult.getReachableClassNames(),
                    treeShakeResult.getReferencedJdkPackages()).shake();
        }

        return new ApplicationModuleInfoBuildItem(appModuleModel);
    }

    private static final Set<String> bootLayerNames = ModuleLayer.boot().modules().stream().map(Module::getName)
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> bootAlwaysExclude = Set.of(
            "io.smallrye.common.annotation");

    /**
     * Add a module to the boot path, including its transitive dependency set.
     * The Quarkus module dependency model is more flexible than the core JVM model, so this
     * set should be as minimal as possible to avoid problems due to cycles and so forth.
     *
     * @param toAdd the module to add (must not be {@code null})
     * @param modulesByName the (read-only) index of modules by name (must not be {@code null})
     * @param visited the visited set (must not be {@code null})
     * @param set the (writable) set of boot modules (must not be {@code null})
     * @return {@code true} if further recursion is needed, or {@code false} to bail out
     */
    private static boolean computeBootPath(ModuleInfo toAdd, final Map<String, ModuleInfo> modulesByName,
            final HashSet<String> visited, final Set<ModuleInfo> set) {
        if (visited.add(toAdd.name())) {
            if (bootAlwaysExclude.contains(toAdd.name())) {
                log.infof("Excluding %s from boot path because it is marked to always be excluded", toAdd.name());
                // but allow dependents to be included
                return true;
            }
            if (toAdd.modifiers().contains(ModuleDescriptor.Modifier.AUTOMATIC)
                    // TODO: remove this once this is modular
                    && !toAdd.name().equals("org.jboss.logmanager.slf4j")) {
                // exclude automatic
                log.infof("Excluding %s from boot path because it is automatic", toAdd.name());
                return false;
            }
            // add its dependencies
            boolean add = true;
            for (DependencyInfo dep : toAdd.dependencies()) {
                if (dep.modifiers().contains(Modifier.LINKED)) {
                    if (!computeBootPath(modulesByName, visited, set, dep)) {
                        add = false;
                        break;
                    }
                }
            }
            if (add) {
                for (AutoDependencyGroup grp : toAdd.autoDependencies()) {
                    for (DependencyInfo dep : grp.dependencies()) {
                        if (dep.modifiers().contains(Modifier.LINKED)) {
                            if (!computeBootPath(modulesByName, visited, set, dep)) {
                                add = false;
                                break;
                            }
                        }
                    }
                }
            }
            if (add) {
                set.add(toAdd);
            } else {
                log.infof("Excluding %s from boot path because a dependency was excluded", toAdd.name());
            }
            return add;
        }
        if (set.contains(toAdd)) {
            return true;
        } else {
            log.infof("Excluding %s from boot path because it is cyclical", toAdd.name());
            return false;
        }
    }

    /**
     * An internal recursion partner for {@link #computeBootPath(ModuleInfo, Map, HashSet, Set)}.
     * This method should not be called directly outside of that method.
     *
     * @param modulesByName the (read-only) index of modules by name (must not be {@code null})
     * @param visited the visited set (must not be {@code null})
     * @param set the (writable) set of boot modules (must not be {@code null})
     * @param dep the dependency to consider (must not be {@code null})
     * @return {@code true} if further recursion is needed, or {@code false} to bail out
     */
    private static boolean computeBootPath(final Map<String, ModuleInfo> modulesByName, final HashSet<String> visited,
            final Set<ModuleInfo> set, final DependencyInfo dep) {
        String name = dep.moduleName();
        // auto-include JDK modules
        if (bootLayerNames.contains(name)) {
            return true;
        }
        ModuleInfo depStuff = modulesByName.get(name);
        if (depStuff != null) {
            return computeBootPath(depStuff, modulesByName, visited, set);
        }
        return true;
    }

    /**
     * Read and tabulate a services file given an input stream in UTF-8 format.
     *
     * @param stream the input stream (must not be {@code null})
     * @param output the collection to write to (must not be {@code null})
     * @return the collection passed in to {@code output} (not {@code null})
     * @param <C> the collection type
     * @throws IOException if the services file could not be read
     */
    private static <C extends Collection<String>> C readServicesFile(InputStream stream, C output) throws IOException {
        return readServicesFile(new InputStreamReader(stream, StandardCharsets.UTF_8), output);
    }

    /**
     * Read and tabulate a services file given a reader.
     *
     * @param reader the reader (must not be {@code null})
     * @param output the collection to write to (must not be {@code null})
     * @return the collection passed in to {@code output} (not {@code null})
     * @param <C> the collection type
     * @throws IOException if the services file could not be read
     */
    private static <C extends Collection<String>> C readServicesFile(Reader reader, C output) throws IOException {
        return reader instanceof BufferedReader br ? readServicesFile(br, output)
                : readServicesFile(new BufferedReader(reader), output);
    }

    /**
     * Read and tabulate a services file given a buffered reader.
     *
     * @param reader the buffered reader (must not be {@code null})
     * @param output the collection to write to (must not be {@code null})
     * @return the collection passed in to {@code output} (not {@code null})
     * @param <C> the collection type
     * @throws IOException if the services file could not be read
     */
    private static <C extends Collection<String>> C readServicesFile(BufferedReader reader, C output) throws IOException {
        String impl;
        while ((impl = reader.readLine()) != null) {
            int octothorpe = impl.indexOf('#');
            if (octothorpe != -1) {
                impl = impl.substring(0, octothorpe);
            }
            impl = impl.trim();
            if (!impl.isEmpty()) {
                int lastDot = impl.lastIndexOf('.');
                if (lastDot != -1) {
                    output.add(impl);
                }
            }
        }
        return output;
    }

    /***
     * Try to locate a {@code module-info} model in a content tree by path.
     *
     * @param contentTree the content tree (must not be {@code null})
     * @param pathName the path name (must not be {@code null})
     * @return the parsed {@code module-info} model.
     */
    private static ClassModel findModuleInfoClass(final PathTree contentTree, final String pathName) {
        return contentTree.apply(pathName, v -> {
            if (v == null) {
                return null;
            }
            try (InputStream is = v.getUrl().openStream()) {
                return ClassFile.of().parse(is.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Compute the Quarkus module descriptor for a given artifact.
     * If the artifact does not have a {@code module-info} descriptor,
     * then its Quarkus module descriptor will be computed as if
     * the artifact is an automatic module using its transitive artifact dependency graph.
     *
     * @param dependency the dependency to process (must not be {@code null})
     * @param generatedByPackageAndPath the (temporary) index of generated classes and resource by package and path name
     * @param moduleInfoClassModels the (read-only) cache of loaded {@code module-info} models by dependency (must not be
     *        {@code null})
     * @param allDeps the (read-only) index of all known dependencies by group and artifact ID (must not be {@code null})
     * @param initialPackages the (read-only) map of initial packages to add to this module (must not be {@code null})
     * @param knownNamedModules the (read-only) map of artifacts by module name (must not be {@code null})
     * @param transformation a transformation operation to apply before returning the descriptor (must not be {@code null})
     * @return the computed module descriptor (not {@code null})
     */
    private static ModuleInfo getModuleInfo(
            ResolvedDependency dependency,
            Map<String, Map<String, Resource>> generatedByPackageAndPath,
            Map<ResolvedDependency, ClassModel> moduleInfoClassModels,
            Map<ArtifactKey, ResolvedDependency> allDeps,
            Map<String, PackageInfo> initialPackages,
            Map<String, ResolvedDependency> knownNamedModules,
            UnaryOperator<ModuleInfo> transformation) {

        PathTree contentTree = dependency.getContentTree();
        ClassModel cm = moduleInfoClassModels.get(dependency);

        String moduleName = dependency.getModuleName();
        String moduleVersion = dependency.getVersion();
        String mainClass = null;

        ModuleDescriptor.Modifier.Set flags = ModuleDescriptor.Modifier.Set.of();
        final Map<String, PackageInfo> packages = new HashMap<>(initialPackages);

        Set<String> uses;
        Map<String, List<String>> provides;
        List<DependencyInfo> requires = new ArrayList<>();
        Map<String, Map<String, PackageAccess>> depAccesses = new HashMap<>();
        List<AutoDependencyGroup> autoDeps = List.of();

        // This temporary measure patches modules which have dependency issues
        // and/or which need to utilize `AddDependencyBuildItem` but do not yet.
        // todo: this will be removed; do not add to it if you can fix the extension instead.
        switch (moduleName) {
            case "io.quarkus.vertx" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .put("io.vertx.core.impl", PackageAccess.EXPORTED);
            }
            case "io.quarkus.resteasy.reactive.vertx" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of("io.vertx.core.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.impl.buffer", PackageAccess.EXPORTED,
                                "io.vertx.core.buffer.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.http.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.net.impl", PackageAccess.EXPORTED));
            }
            case "io.smallrye.common.vertx" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .put("io.vertx.core.impl", PackageAccess.EXPORTED);
            }
            case "io.quarkus.rest.client.jaxrs" -> {
                depAccesses.computeIfAbsent("io.quarkus.resteasy.reactive.client", ModularitySteps::newMap)
                        .put("org.jboss.resteasy.reactive.client.impl", PackageAccess.EXPORTED);
            }
            case "io.quarkus.smallrye.context.propagation" -> {
                depAccesses.computeIfAbsent("io.smallrye.context.propagation", ModularitySteps::newMap)
                        .put("io.smallrye.context.impl", PackageAccess.EXPORTED);
            }
            case "io.smallrye.mutiny.vertx.core" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .put("io.vertx.core.shareddata.impl", PackageAccess.EXPORTED);
            }
            case "io.quarkus.oidc" -> {
                depAccesses.computeIfAbsent("io.vertx.web", ModularitySteps::newMap)
                        .put("io.vertx.ext.web.impl", PackageAccess.EXPORTED);
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of(
                                "io.vertx.core.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.http.impl", PackageAccess.EXPORTED));
            }
            case "io.quarkus.vertx.http" -> {
                depAccesses.computeIfAbsent("io.vertx.web", ModularitySteps::newMap)
                        .put("io.vertx.ext.web.impl", PackageAccess.EXPORTED);
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of(
                                "io.vertx.core.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.http.impl", PackageAccess.EXPORTED));
            }
            case "io.vertx.web" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of(
                                "io.vertx.core.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.http.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.net.impl", PackageAccess.EXPORTED));
            }
            case "io.vertx.web.client" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of(
                                "io.vertx.core.impl.launcher.commands", PackageAccess.EXPORTED,
                                "io.vertx.core.impl", PackageAccess.EXPORTED,
                                "io.vertx.core.http.impl", PackageAccess.EXPORTED));
                depAccesses.computeIfAbsent("io.vertx.web.common", ModularitySteps::newMap)
                        .put("io.vertx.ext.web.codec.impl", PackageAccess.EXPORTED);
            }
            case "org.jboss.threads" -> {
                depAccesses.computeIfAbsent("java.base", ModularitySteps::newMap)
                        .put("java.lang", PackageAccess.OPEN);
            }
            case "org.resteasy.core" -> {
                depAccesses.computeIfAbsent("com.fasterxml.jackson.jakarta.rs.json", ModularitySteps::newMap)
                        .put("com.fasterxml.jackson.jakarta.rs.json", PackageAccess.OPEN);
            }
        }

        // Now build the module info. There are two possibilities:
        //   • We have a `module-info.class` and should build a regular module from that
        //   • We do not have `module-info` so we build an automatic module from Maven dependencies
        if (cm != null) {
            // there is a descriptor
            Optional<RuntimeInvisibleAnnotationsAttribute> ria = cm.findAttribute(Attributes.runtimeInvisibleAnnotations());
            if (ria.isPresent()) {
                // process annotations from the module declaration
                RuntimeInvisibleAnnotationsAttribute riaa = ria.get();
                for (Annotation a : riaa.annotations()) {
                    switch (a.className().stringValue()) {
                        case "io.smallrye.common.annotation.AddExports" ->
                            processAccessAnnotation(a, depAccesses, PackageAccess.EXPORTED);
                        case "io.smallrye.common.annotation.AddOpens" ->
                            processAccessAnnotation(a, depAccesses, PackageAccess.OPEN);
                        case "io.smallrye.common.annotation.AddExports$List" ->
                            processAccessAnnotationList(a, depAccesses, PackageAccess.EXPORTED);
                        case "io.smallrye.common.annotation.AddOpens$List" ->
                            processAccessAnnotationList(a, depAccesses, PackageAccess.OPEN);
                        case "io.smallrye.common.annotation.NativeAccess" ->
                            flags = flags.with(ModuleDescriptor.Modifier.NATIVE_ACCESS);
                    }
                }
            }
            // register the main class
            mainClass = cm.findAttribute(Attributes.moduleMainClass()).map(ModuleMainClassAttribute::mainClass)
                    .map(ClassEntry::name)
                    .map(Utf8Entry::stringValue)
                    .map(n -> n.replace('/', '.'))
                    .orElse(null);
            // figure out which packages we open to everyone
            Set<String> openedPackages = cm.findAttribute(Attributes.module()).map(ModuleAttribute::opens)
                    .map(l -> l.stream()
                            .filter(ei -> ei.opensTo().isEmpty())
                            .map(ei -> ei.openedPackage().name().stringValue().replace('/', '.'))
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            // figure out which packages we export to everyone
            Set<String> exportedPackages = cm.findAttribute(Attributes.module()).map(ModuleAttribute::exports)
                    .map(l -> l.stream()
                            .filter(ei -> ei.exportsTo().isEmpty())
                            .map(ei -> ei.exportedPackage().name().stringValue().replace('/', '.'))
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            // compute a PackageInfo for every package, merging in exported and opened package information
            cm.findAttribute(Attributes.modulePackages()).ifPresentOrElse(a -> a.packages().forEach(pe -> {
                String pn = pe.name().stringValue().replace('/', '.');
                PackageInfo info = openedPackages.contains(pn) ? PackageInfo.OPEN
                        : exportedPackages.contains(pn) ? PackageInfo.EXPORTED : PackageInfo.PRIVATE;
                packages.compute(pn, (ignored, old) -> PackageInfo.merge(info, old));
            }), () -> indexPackages(packages, contentTree));
            // find the set of used services
            uses = cm.findAttribute(Attributes.module()).map(ModuleAttribute::uses).orElse(List.of()).stream()
                    .map(ClassEntry::name)
                    .map(Utf8Entry::stringValue)
                    .map(c -> c.replace('/', '.'))
                    .collect(Collectors.toUnmodifiableSet());
            // find the set of provided services
            provides = cm.findAttribute(Attributes.module()).map(ModuleAttribute::provides).orElse(List.of()).stream()
                    .map(mpi -> Map.entry(
                            mpi.provides().name().stringValue().replace('/', '.'),
                            mpi.providesWith().stream().map(ClassEntry::name).map(Utf8Entry::stringValue)
                                    .map(e -> e.replace('/', '.')).toList()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            // find the dependency set, marking things `optional` if they are missing
            cm.findAttribute(Attributes.module()).map(ModuleAttribute::requires).orElse(List.of()).stream()
                    .map(mri -> Map.entry(
                            mri.requires().name().stringValue(),
                            mri.requiresFlags()))
                    .forEach(e -> {
                        Modifier.Set modifiers = depModifiersOf(e.getValue());
                        String depName = e.getKey();
                        if (!bootLayerNames.contains(depName) && !knownNamedModules.containsKey(depName)) {
                            // it's actually totally missing...
                            modifiers = modifiers.with(Modifier.OPTIONAL);
                        }
                        requires.add(new DependencyInfo(depName, modifiers,
                                depAccesses.getOrDefault(depName, Map.of())));
                    });
        } else {
            // create an automatic module
            flags = flags.with(ModuleDescriptor.Modifier.AUTOMATIC);
            // process the manifest for standard attributes
            ManifestAttributes manifestAttributes = contentTree.getManifestAttributes();
            if (manifestAttributes != null) {
                // the main class
                mainClass = manifestAttributes.mainClassName();
                // add-opens
                manifestAttributes.addOpens().forEach((mn, pl) -> {
                    Map<String, PackageAccess> subMap = depAccesses.computeIfAbsent(mn, ModularitySteps::newMap);
                    pl.forEach(pn -> subMap.put(pn, PackageAccess.OPEN));
                });
                // add-exports
                manifestAttributes.addExports().forEach((mn, pl) -> {
                    Map<String, PackageAccess> subMap = depAccesses.computeIfAbsent(mn, ModularitySteps::newMap);
                    pl.forEach(pn -> subMap.putIfAbsent(pn, PackageAccess.EXPORTED));
                });
                // enable-native-access
                if (manifestAttributes.enableNativeAccess()) {
                    flags = flags.with(ModuleDescriptor.Modifier.NATIVE_ACCESS);
                }
            }
            // create an index of all of the packages based on the content tree
            indexPackages(packages, contentTree);
            // process the service files into a service provider map
            provides = contentTree.apply("META-INF/services", pv -> {
                if (pv == null) {
                    return Map.of();
                }
                Map<String, List<String>> services = new HashMap<>();
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(pv.getPath())) {
                    for (Path svc : ds) {
                        if (Files.isRegularFile(svc)) {
                            services.put(svc.getFileName().toString(),
                                    readServicesFile(Files.newBufferedReader(svc, StandardCharsets.UTF_8), new ArrayList<>()));
                        }
                    }
                } catch (IOException ignored) {
                }
                return Map.copyOf(services);
            });
            // automatic modules automatically use all services
            uses = Set.of();
            // automatic module implicit requirements; the rest come from Maven
            requires.add(new DependencyInfo("java.base", Modifier.Set.of(Modifier.LINKED,
                    Modifier.READ, Modifier.SERVICES, Modifier.MANDATED), Map.of()));
            // todo: ideally we could avoid requiring `java.se` even for automatic modules thru dep analysis
            requires.add(new DependencyInfo("java.se", Modifier.Set.of(Modifier.LINKED,
                    Modifier.READ, Modifier.SERVICES, Modifier.SYNTHETIC), Map.of()));
            // compute automatic dependencies
            // NOTE: this includes each artifact in the transitive graph; this is NOT THE SAME
            // as marking the module dependency as `TRANSITIVE` and cannot be replaced with doing so.
            autoDeps = computeAutomaticDependencies(moduleName, dependency,
                    allDeps, depAccesses);
        }
        // Add generated resources that were previously indexed for us (#44657)
        // NOTE: modifies the generated packages map
        List<Resource> generated = packages.keySet().stream()
                .map(generatedByPackageAndPath::remove)
                .filter(Objects::nonNull)
                .map(Map::values)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Resource::pathName))
                .collect(Collectors.toList());

        ModuleInfo mi = new ModuleInfo(moduleName, moduleVersion, flags, dependency, mainClass, packages, requires,
                autoDeps, uses, provides, generated);
        return transformation.apply(mi);
    }

    /**
     * A work list item for the recursive automatic dependency processing algorithm.
     *
     * @param moduleName the module name to depend on (must not be {@code null})
     * @param artifact the artifact of the dependency module (must not be {@code null})
     * @param depth the recursion depth
     * @param optional {@code true} if the dependency should be optional, or {@code false} if it should be required
     */
    private record WorkListItem(String moduleName, ResolvedDependency artifact, int depth, boolean optional) {
    }

    /**
     * The entry point for automatic dependency processing.
     * The dependencies are assembled into "groups" so that we can add comments before each group to understand
     * where each dependency came from.
     *
     * @param moduleName the name of the module being considered (must not be {@code null})
     * @param rootArtifact the artifact of the module being considered (must not be {@code null})
     * @param allDeps the map of all dependencies in the project (must not be {@code null})
     * @param depAccesses the dependency extra accesses map
     * @return the list of automatic dependency groups (not {@code null})
     */
    private static List<AutoDependencyGroup> computeAutomaticDependencies(String moduleName, ResolvedDependency rootArtifact,
            final Map<ArtifactKey, ResolvedDependency> allDeps,
            final Map<String, Map<String, PackageAccess>> depAccesses) {
        HashSet<ArtifactKey> visited = new HashSet<>();
        // ignore any cyclic reference
        visited.add(keyOf(rootArtifact));
        final ArrayDeque<WorkListItem> workList = new ArrayDeque<>();
        ArrayList<AutoDependencyGroup> groups = new ArrayList<>();
        // add the initial work list entry
        workList.add(new WorkListItem(moduleName, rootArtifact, 0, false));
        // run the work list until all dependencies have been processed
        while (!workList.isEmpty()) {
            WorkListItem item = workList.removeFirst();
            computeAutomaticDependencies(item.moduleName(), item.artifact(), allDeps, visited, workList,
                    groups, item.depth(), item.optional(), depAccesses);
        }
        return groups;
    }

    /**
     * Process automatic module dependencies.
     * This is done by recursively visiting the transitive closure of the module's Maven dependencies (even non-automatic
     * modules),
     * because automatic modules normally can "see" all of these artifacts.
     * Note that this is not exactly the same as traditional {@code module-info} transitive dependencies.
     *
     * @param moduleName the name of the module being considered (must not be {@code null})
     * @param artifact the artifact of the module being considered (must not be {@code null})
     * @param allDeps the map of all dependencies in the project (must not be {@code null})
     * @param visited the visited set of module names (must not be {@code null})
     * @param workList the remaining work list (must not be {@code null})
     * @param groups the groups list (must not be {@code null})
     * @param depth the nesting depth
     * @param optional if this subtree is optional
     * @param depAccesses the dependency extra accesses map
     */
    private static void computeAutomaticDependencies(String moduleName, ResolvedDependency artifact,
            Map<ArtifactKey, ResolvedDependency> allDeps,
            Set<ArtifactKey> visited, ArrayDeque<WorkListItem> workList,
            ArrayList<AutoDependencyGroup> groups, int depth, boolean optional,
            Map<String, Map<String, PackageAccess>> depAccesses) {

        Collection<Dependency> deps = artifact.getDirectDependencies();
        // map and sort dependencies
        final List<DependencyInfo> depList = new ArrayList<>(deps.size());
        for (Dependency dependency : deps) {
            ArtifactKey key = keyOf(dependency);
            if (visited.add(key)) {
                ResolvedDependency resolved = allDeps.get(key);
                if (resolved == null || !resolved.isResolved() || resolved.getContentTree().isEmpty() ||
                        dependency.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)) {
                    if (!dependency.isOptional()) {
                        log.debugf("No dependency found from %s to %s", moduleName, key);
                    }
                    continue;
                }
                if (!dependency.isFlagSet(DependencyFlags.RUNTIME_CP)) {
                    log.debugf("Skipping non-runtime dependency from %s to %s", moduleName, key);
                    continue;
                }
                switch (resolved.getScope()) {
                    case "compile", "provided", "runtime" -> {
                    }
                    default -> {
                        log.debugf("Skipping dependency with scope %s", resolved.getScope());
                        continue;
                    }
                }
                String depName = resolved.getModuleName();
                Modifier.Set mods = Modifier.Set.of(Modifier.LINKED, Modifier.READ, Modifier.SERVICES);
                if (optional || dependency.isOptional() ||
                        dependency.getScope().equals("provided") ||
                        depth > 1) {
                    mods = mods.with(Modifier.OPTIONAL);
                }
                depList.add(new DependencyInfo(
                        depName,
                        mods,
                        depAccesses.getOrDefault(depName, Map.of())));
                workList.addLast(new WorkListItem(depName, resolved, depth + 1, optional || mods.contains(Modifier.OPTIONAL)));
            }
        }
        if (!depList.isEmpty()) {
            depList.sort(Comparator.comparing(DependencyInfo::moduleName));
            AutoDependencyGroup group = new AutoDependencyGroup(
                    moduleName,
                    depList);
            groups.add(group);
        }
    }

    /**
     * Create a usable artifact key from a dependency or its coordinates.
     *
     * @param dependency the coordinates object (must not be {@code null})
     * @return the artifact key (group, artifact, and classifier only) (not {@code null})
     */
    private static ArtifactKey keyOf(final ArtifactCoords dependency) {
        return ArtifactKey.gac(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier());
    }

    /**
     * Convert JPMS access flags to Quarkus module modifiers.
     *
     * @param flags the JPMS access flags set (must not be {@code null})
     * @return the Quarkus module modifiers (not {@code null})
     */
    private static Modifier.Set depModifiersOf(final Set<AccessFlag> flags) {
        Modifier.Set set = Modifier.Set.of(Modifier.LINKED, Modifier.READ,
                Modifier.SERVICES);
        if (flags.contains(AccessFlag.STATIC_PHASE)) {
            set = set.with(Modifier.OPTIONAL);
        }
        if (flags.contains(AccessFlag.TRANSITIVE)) {
            set = set.with(Modifier.TRANSITIVE);
        }
        if (flags.contains(AccessFlag.SYNTHETIC)) {
            set = set.with(Modifier.SYNTHETIC);
        }
        if (flags.contains(AccessFlag.MANDATED)) {
            set = set.with(Modifier.MANDATED);
        }
        return set;
    }

    /**
     * Update the given package index based on discovered packages in an automatic module.
     *
     * @param packages the packages map to update (must not be {@code null})
     * @param content the artifact content (must not be {@code null})
     */
    private static void indexPackages(final Map<String, PackageInfo> packages, final PathTree content) {
        content.walk(vp -> {
            String rp = vp.getResourceName();
            // flatten MR-JAR structure.
            if (rp.startsWith("META-INF/versions/")) {
                int idx = rp.indexOf('/', 18);
                if (idx == -1) {
                    // skip weird entry
                    return;
                }
                rp = rp.substring(idx + 1);
            }
            // A directory is a package if it contains a class.
            if (rp.endsWith(".class")) {
                int idx = rp.lastIndexOf('/');
                if (idx != -1) {
                    String pkgName = rp.substring(0, idx).replace('/', '.');
                    // A package is public if it does not contain a segment named `impl`, `private_`, or `_private`.
                    packages.compute(pkgName, (n, old) -> {
                        if (n.endsWith(".impl") || n.contains(".impl.")
                                || n.endsWith(".private_") || n.contains(".private_.")
                                || n.endsWith("._private") || n.contains("._private.")) {
                            return PackageInfo.merge(old, PackageInfo.PRIVATE);
                        } else {
                            return PackageInfo.merge(old, PackageInfo.EXPORTED);
                        }
                    });
                }
            }
        });
    }

    /**
     * Process one of the access annotations: {@code io.smallrye.common.annotation.AddOpens}
     * or {@code io.smallrye.common.annotation.AddExports}.
     *
     * @param ann the annotation (must not be {@code null})
     * @param depAccesses the writable map of package accesses (must not be {@code null})
     * @param access the access to grant (must not be {@code null}; based on the annotation type)
     */
    private static void processAccessAnnotation(Annotation ann, Map<String, Map<String, PackageAccess>> depAccesses,
            PackageAccess access) {
        String moduleName = null;
        Set<String> packages = null;
        for (AnnotationElement element : ann.elements()) {
            switch (element.name().stringValue()) {
                case "module" -> moduleName = ((AnnotationValue.OfString) element.value()).stringValue();
                case "packages" -> packages = ((AnnotationValue.OfArray) element.value()).values().stream()
                        .map(AnnotationValue.OfString.class::cast).map(AnnotationValue.OfString::stringValue)
                        .collect(Collectors.toSet());
            }
        }
        if (moduleName == null || moduleName.equals("ALL-UNNAMED") || packages == null) {
            // ignore invalid annotation
            return;
        }
        Map<String, PackageAccess> accesses = depAccesses.computeIfAbsent(moduleName, ModularitySteps::newMap);
        switch (access) {
            case OPEN -> packages.forEach(pn -> accesses.put(pn, PackageAccess.OPEN));
            case EXPORTED -> packages.forEach(pn -> accesses.putIfAbsent(pn, PackageAccess.EXPORTED));
        }
    }

    /**
     * {@return a new map for use in Map.compute*() operations}
     */
    private static <K, V> Map<K, V> newMap(Object ignored) {
        return new HashMap<>();
    }

    /**
     * {@return a new list for use in Map.compute*() operations}
     */
    private static <E> List<E> newList(Object ignored) {
        return new ArrayList<>();
    }

    /**
     * Process an access annotation list.
     *
     * @param ann the list annotation (must not be {@code null})
     * @param depAccesses the writable map of package accesses (must not be {@code null})
     * @param access the access to grant (must not be {@code null}; based on the annotation type)
     */
    private static void processAccessAnnotationList(Annotation ann, Map<String, Map<String, PackageAccess>> depAccesses,
            PackageAccess access) {
        for (AnnotationElement element : ann.elements()) {
            switch (element.name().stringValue()) {
                case "value" ->
                    processAccessAnnotation(((AnnotationValue.OfAnnotation) element.value()).annotation(), depAccesses, access);
            }
        }
    }

}
