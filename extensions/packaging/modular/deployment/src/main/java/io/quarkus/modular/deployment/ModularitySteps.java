package io.quarkus.modular.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.modular.spi.items.AddedDependencyBuildItem;
import io.quarkus.modular.spi.items.ApplicationModuleInfoBuildItem;
import io.quarkus.modular.spi.items.BootModulePathBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.AutoDependencyGroup;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
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
import io.smallrye.modules.desc.Modifiers;
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
    public List<AddedDependencyBuildItem> standardAddedDependencies(
            CurateOutcomeBuildItem curateOutcome) {
        // TODO: migrate these to their relevant extensions
        return List.of(
                new AddedDependencyBuildItem("org.eclipse.microprofile.config", "io.smallrye.config",
                        Modifier.set(Modifier.SERVICES)),
                // todo: this one must be READ and LINKED because an ArC synthetic bean requires it
                new AddedDependencyBuildItem("io.netty.transport", "io.quarkus.netty",
                        Modifier.set(Modifier.SERVICES, Modifier.READ, Modifier.LINKED)),
                new AddedDependencyBuildItem("jakarta.ws.rs", "io.quarkus.resteasy.reactive.common",
                        Modifier.set(Modifier.SERVICES, Modifier.OPTIONAL)),
                new AddedDependencyBuildItem("org.slf4j", "org.jboss.logmanager.slf4j", Modifier.set(Modifier.SERVICES)));
    }

    @BuildStep
    public ApplicationModuleInfoBuildItem buildModularityModel(
            CurateOutcomeBuildItem curateOutcome,
            MainClassBuildItem mainClassBuildItem,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            TransformedClassesBuildItem transformedClasses,
            List<ModuleOpenBuildItem> opens,
            // TODO: List<ModuleExportBuildItem> exports,
            List<ModuleEnableNativeAccessBuildItem> nativeAccesses,
            List<AddedDependencyBuildItem> extraDeps,
            List<BootModulePathBuildItem> bootPathItems) {
        ApplicationModel model = curateOutcome.getApplicationModel();
        // construct complete module information for the entire project
        ResolvedDependency appArtifact = model.getAppArtifact();

        // tabulate all generated and transformed resources, by package, so we can assign them to modules
        Map<String, Map<String, Resource>> generatedByPackageAndPath = new HashMap<>();
        // the set of packages containing ArC generated classes
        // todo: replace this with some build item that adds the dependency
        Set<String> arcGeneratedPackages = new HashSet<>();
        Set<String> arcRuntimeGeneratedPackages = new HashSet<>();
        Set<String> extraAppModuleDepPackages = new HashSet<>();
        Set<String> bootPathModules = bootPathItems.stream().map(BootModulePathBuildItem::moduleName)
                .collect(Collectors.toSet());
        Map<String, ResolvedDependency> knownNamedModules = model.getDependencies().stream()
                .filter(d -> d.isFlagSet(DependencyFlags.RUNTIME_CP))
                .filter(d -> !d.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION))
                .map(d -> Map.entry(d.getModuleName(), d))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Map<String, Modifiers<Modifier>>> extraDepsMap = extraDeps.stream()
                .collect(Collectors.groupingBy(
                        AddedDependencyBuildItem::module,
                        Collectors.groupingBy(
                                AddedDependencyBuildItem::targetModule,
                                Collectors.mapping(AddedDependencyBuildItem::modifiers,
                                        Collectors.reducing(Modifier.set(), Modifiers::withAll)))));
        // opening module -> opened module -> package set
        Map<String, Map<String, Set<String>>> addOpensByModule = opens.stream()
                .collect(Collectors.groupingBy(
                        ModuleOpenBuildItem::openingModuleName,
                        Collectors.groupingBy(
                                ModuleOpenBuildItem::openedModuleName,
                                Collectors.flatMapping(mobi -> mobi.packageNames().stream(),
                                        Collectors.toSet()))));
        Set<String> nativeAccessNames = nativeAccesses.stream()
                .map(ModuleEnableNativeAccessBuildItem::moduleName)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, String> modulesByPackageTemporary = new HashMap<>();

        for (GeneratedClassBuildItem gcbi : generatedClasses) {
            String pn = gcbi.packageName();
            String bn = gcbi.binaryName();
            String name = gcbi.internalName() + ".class";
            // todo: change GeneratedClassBuildItem to use Resource instead of byte[]
            generatedByPackageAndPath.computeIfAbsent(pn, ModularitySteps::newMap)
                    .put(name, new MemoryResource(name, gcbi.getClassData()));
            // TODO: We need a better way to detect ArC dependencies using AddedDependencyBuildItem
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
        int misLen = "META-INF/services".length();
        // impl package -> service name -> impl name
        Map<String, Map<String, List<String>>> extraServicesByPackage = new HashMap<>();
        for (GeneratedResourceBuildItem rsrc : generatedResources) {
            String name = rsrc.getName();
            if (name.startsWith("META-INF/services/")) {
                // todo: generate service build items instead - io.quarkus.arc.deployment.ArcProcessor.generateResources
                if (name.lastIndexOf('/') == misLen) {
                    String apiName = name.substring(misLen + 1);
                    ArrayList<String> impls;
                    try {
                        impls = readServicesFile(new ByteArrayInputStream(rsrc.getData()), new ArrayList<>());
                    } catch (IOException unexpected) {
                        throw new IllegalStateException(unexpected);
                    }
                    for (String impl : impls) {
                        int lastDot = impl.lastIndexOf('.');
                        if (lastDot != -1) {
                            String pkgName = impl.substring(0, lastDot);
                            extraServicesByPackage
                                    .computeIfAbsent(pkgName, ignored -> new HashMap<>())
                                    .computeIfAbsent(apiName, ignored -> new ArrayList<>())
                                    .add(impl);
                        }
                    }
                }
                // else ignore
            } else {
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
        }
        for (Set<TransformedClassesBuildItem.TransformedClass> set : transformedClasses.getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass tc : set) {
                String bn = tc.getClassName();
                String pn;
                int idx = bn.lastIndexOf('.');
                if (idx == -1) {
                    pn = "";
                } else {
                    pn = bn.substring(0, idx);
                }
                // replace any existing entry
                String fileName = tc.getFileName();
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

        Map<ArtifactKey, ResolvedDependency> allDependenciesByKey = knownNamedModules.values().stream().map(
                d -> Map.entry(keyOf(d), d)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

        final String appModuleName = appArtifact.getModuleName();

        // ArC must depend on the application module (for service loading)
        // todo: find a better solution
        extraDepsMap.computeIfAbsent("io.quarkus.arc", k -> new HashMap<>())
                .put(appModuleName, Modifier.set(Modifier.SERVICES));

        extraDepsMap.computeIfAbsent("io.vertx.core", k -> new HashMap<>())
                .put("io.quarkus.vertx", Modifier.set(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC));

        for (ResolvedDependency dependency : knownNamedModules.values()) {
            if (!dependency.isRuntimeCp()) {
                continue;
            }
            String moduleName = dependency.getModuleName();
            if (moduleName.equals(appModuleName)) {
                // we do app module at the end
                continue;
            }
            final ModuleInfo depModule = getModuleInfo(dependency, generatedByPackageAndPath, moduleInfoClassModels,
                    allDependenciesByKey, Map.of(), knownNamedModules, mi -> {
                        // add extra services
                        boolean arcAdded = false;
                        boolean arcRuntimeAdded = false;
                        if (nativeAccessNames.contains(moduleName)) {
                            mi = mi.withModifier(ModuleDescriptor.Modifier.NATIVE_ACCESS);
                        }
                        for (String pkg : mi.packages().keySet()) {
                            if (extraServicesByPackage.containsKey(pkg)) {
                                mi = mi.withMoreServices(extraServicesByPackage.remove(pkg));
                            }
                            // TODO: use build items to do this more efficiently up front
                            if (!arcAdded && arcGeneratedPackages.contains(pkg)) {
                                if (!mi.name().equals("io.quarkus.arc")) {
                                    mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                            "io.quarkus.arc", Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                            Map.of())));
                                }
                                if (!mi.name().equals("jakarta.cdi")) {
                                    mi = mi.withMoreDependencies(List.of(new DependencyInfo("jakarta.cdi",
                                            Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                            Map.of())));
                                }
                                arcAdded = true;
                            }
                            if (!arcRuntimeAdded && arcRuntimeGeneratedPackages.contains(pkg)
                                    && !mi.name().equals("io.quarkus.arc.runtime")) {
                                mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                        "io.quarkus.arc.runtime",
                                        Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                        Map.of())));
                                arcRuntimeAdded = true;
                            }
                        }
                        switch (moduleName) {
                            // todo: needs resteasy release
                            case "org.jboss.resteasy.core.spi" -> {
                                mi = mi.withMorePackages(Map.of("org.jboss.resteasy.resteasy_jaxrs.i18n", PackageInfo.of(
                                        PackageAccess.PRIVATE, Set.of(), Set.of("org.jboss.logging"))));
                            }
                        }
                        // add extra dependencies
                        mi = mi.withMoreDependencies(extraDepsMap.getOrDefault(mi.name(), Map.of())
                                .entrySet()
                                .stream()
                                .map((e) -> new DependencyInfo(e.getKey(), e.getValue(), Map.of()))
                                .toList());
                        Map<String, Set<String>> obi = addOpensByModule.getOrDefault(moduleName, Map.of());
                        if (!obi.isEmpty()) {
                            mi = mi.withMoreDependencies(obi
                                    .entrySet()
                                    .stream()
                                    .map((e) -> new DependencyInfo(
                                            e.getKey(),
                                            Modifier.set(Modifier.READ, Modifier.OPTIONAL),
                                            e.getValue().stream().collect(
                                                    Collectors.toMap(Function.identity(), ignored -> PackageAccess.OPEN))))
                                    .toList());
                        }
                        return mi;
                    });
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
            depModule.packages().keySet().forEach(pn -> {
                String existing = modulesByPackageTemporary.putIfAbsent(pn, moduleName);
                if (existing != null && !existing.equals(moduleName)) {
                    log.warnf("Package %s is split, in %s and %s", pn, existing, moduleName);
                }
            });
        }

        for (String pn : extraAppModuleDepPackages) {
            String moduleName = modulesByPackageTemporary.get(pn);
            if (moduleName != null) {
                extraDepsMap.computeIfAbsent(appModuleName, ModularitySteps::newMap)
                        .compute(moduleName,
                                (pn1, mods) -> mods == null ? Modifier.set(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC)
                                        : mods.withAll(Modifier.READ, Modifier.LINKED));
            }
        }

        Map<String, PackageInfo> initialPackages = new HashMap<>(Map.of(
                "", PackageInfo.PRIVATE,
                "io.quarkus.runner", PackageInfo.EXPORTED,
                "io.quarkus.runtime.generated", PackageInfo.EXPORTED,
                "io.quarkus.deployment.steps", PackageInfo.EXPORTED,
                "io.quarkus.rest.runtime", PackageInfo.EXPORTED,
                "io.quarkus.arc.setup", PackageInfo.EXPORTED,
                "io.quarkus.arc.generator", PackageInfo.EXPORTED,
                "io.quarkus.runner.recorded", PackageInfo.EXPORTED));

        // claim all unclaimed generated classes and resources
        generatedByPackageAndPath.keySet().forEach(pn -> initialPackages.putIfAbsent(pn, PackageInfo.PRIVATE));

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
                                    "io.quarkus.arc", Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                    Map.of())));
                            arcAdded = true;
                        }
                        if (!arcRuntimeAdded && arcRuntimeGeneratedPackages.contains(pkg)) {
                            mi = mi.withMoreDependencies(List.of(new DependencyInfo(
                                    "io.quarkus.arc.runtime", Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SYNTHETIC),
                                    Map.of())));
                            arcRuntimeAdded = true;
                        }
                    }
                    // now add all extra dependencies
                    mi = mi.withMoreDependencies(extraDepsMap.getOrDefault(mi.name(), Map.of())
                            .entrySet()
                            .stream()
                            .map((e) -> new DependencyInfo(e.getKey(), e.getValue(), Map.of()))
                            .toList());
                    return mi;
                });

        modulesByName.put(appModuleName, appModule);

        // -----------------------------------
        // at this point, appModule is frozen!
        // -----------------------------------

        // determine the set of boot-path modules
        // todo: this is extension-dependent
        Set<ModuleInfo> bootModuleSet = new HashSet<>();
        HashSet<String> visited = new HashSet<>();
        for (ModuleInfo moduleInfo : modulesByName.values()) {
            if (bootPathModules.contains(moduleInfo.name())) {
                computeBootPath(moduleInfo, modulesByName, visited, bootModuleSet);
            }
        }
        AppModuleModel.Builder amb = AppModuleModel.builder();
        amb.appModuleInfo(appModule);
        bootModuleSet.forEach(m -> amb.bootModule(m.name()));
        modulesByName.values().forEach(amb::moduleInfo);
        return new ApplicationModuleInfoBuildItem(amb.build());
    }

    private static final Set<String> bootLayerNames = ModuleLayer.boot().modules().stream().map(Module::getName)
            .collect(Collectors.toUnmodifiableSet());

    private static boolean computeBootPath(ModuleInfo toAdd, final Map<String, ModuleInfo> modulesByArtifact,
            final HashSet<String> visited, final Set<ModuleInfo> set) {
        if (visited.add(toAdd.name())) {
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
                    if (!computeBootPath(modulesByArtifact, visited, set, dep)) {
                        add = false;
                        break;
                    }
                }
            }
            if (add) {
                for (AutoDependencyGroup grp : toAdd.autoDependencies()) {
                    for (DependencyInfo dep : grp.dependencies()) {
                        if (dep.modifiers().contains(Modifier.LINKED)) {
                            if (!computeBootPath(modulesByArtifact, visited, set, dep)) {
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

    private static boolean computeBootPath(final Map<String, ModuleInfo> modulesByArtifact, final HashSet<String> visited,
            final Set<ModuleInfo> set, final DependencyInfo dep) {
        String name = dep.moduleName();
        // auto-include JDK modules
        if (bootLayerNames.contains(name)) {
            return true;
        }
        ModuleInfo depStuff = modulesByArtifact.get(name);
        if (depStuff != null) {
            return computeBootPath(depStuff, modulesByArtifact, visited, set);
        }
        return true;
    }

    private static <C extends Collection<String>> C readServicesFile(InputStream stream, C output) throws IOException {
        return readServicesFile(new InputStreamReader(stream, StandardCharsets.UTF_8), output);
    }

    private static <C extends Collection<String>> C readServicesFile(Reader reader, C output) throws IOException {
        return reader instanceof BufferedReader br ? readServicesFile(br, output)
                : readServicesFile(new BufferedReader(reader), output);
    }

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

        Modifiers<ModuleDescriptor.Modifier> flags = ModuleDescriptor.Modifier.set();
        final Map<String, PackageInfo> packages = new HashMap<>(initialPackages);

        Set<String> uses;
        Map<String, List<String>> provides;
        List<DependencyInfo> requires = new ArrayList<>();
        Map<String, Map<String, PackageAccess>> depAccesses = new HashMap<>();
        List<AutoDependencyGroup> autoDeps = List.of();

        // todo: Manual module add-exports/add-opens fixups
        switch (moduleName) {
            case "io.quarkus.vertx" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .put("io.vertx.core.impl", PackageAccess.EXPORTED);
            }
            case "io.quarkus.resteasy.reactive.vertx" -> {
                depAccesses.computeIfAbsent("io.vertx.core", ModularitySteps::newMap)
                        .putAll(Map.of("io.vertx.core.impl", PackageAccess.EXPORTED,
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
                                "io.vertx.core.impl.logging", PackageAccess.EXPORTED,
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

        // now build the module info
        if (cm != null) {
            // there is a descriptor
            Optional<RuntimeInvisibleAnnotationsAttribute> ria = cm.findAttribute(Attributes.runtimeInvisibleAnnotations());
            if (ria.isPresent()) {
                // process annotations
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
            mainClass = cm.findAttribute(Attributes.moduleMainClass()).map(ModuleMainClassAttribute::mainClass)
                    .map(ClassEntry::name)
                    .map(Utf8Entry::stringValue)
                    .map(n -> n.replace('/', '.'))
                    .orElse(null);
            Set<String> openedPackages = cm.findAttribute(Attributes.module()).map(ModuleAttribute::opens)
                    .map(l -> l.stream()
                            .filter(ei -> ei.opensTo().isEmpty())
                            .map(ei -> ei.openedPackage().name().stringValue().replace('/', '.'))
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            Set<String> exportedPackages = cm.findAttribute(Attributes.module()).map(ModuleAttribute::exports)
                    .map(l -> l.stream()
                            .filter(ei -> ei.exportsTo().isEmpty())
                            .map(ei -> ei.exportedPackage().name().stringValue().replace('/', '.'))
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
            cm.findAttribute(Attributes.modulePackages()).ifPresentOrElse(a -> a.packages().forEach(pe -> {
                String pn = pe.name().stringValue().replace('/', '.');
                PackageInfo info = openedPackages.contains(pn) ? PackageInfo.OPEN
                        : exportedPackages.contains(pn) ? PackageInfo.EXPORTED : PackageInfo.PRIVATE;
                packages.compute(pn, (ignored, old) -> PackageInfo.merge(info, old));
            }), () -> indexPackages(packages, contentTree));
            uses = cm.findAttribute(Attributes.module()).map(ModuleAttribute::uses).orElse(List.of()).stream()
                    .map(ClassEntry::name)
                    .map(Utf8Entry::stringValue)
                    .map(c -> c.replace('/', '.'))
                    .collect(Collectors.toUnmodifiableSet());
            provides = cm.findAttribute(Attributes.module()).map(ModuleAttribute::provides).orElse(List.of()).stream()
                    .map(mpi -> Map.entry(
                            mpi.provides().name().stringValue().replace('/', '.'),
                            mpi.providesWith().stream().map(ClassEntry::name).map(Utf8Entry::stringValue)
                                    .map(e -> e.replace('/', '.')).toList()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            cm.findAttribute(Attributes.module()).map(ModuleAttribute::requires).orElse(List.of()).stream()
                    .map(mri -> Map.entry(
                            mri.requires().name().stringValue(),
                            mri.requiresFlags()))
                    .forEach(e -> {
                        Modifiers<Modifier> modifiers = depModifiersOf(e.getValue());
                        String depName = e.getKey();
                        if (!bootLayerNames.contains(depName) && !knownNamedModules.containsKey(depName)) {
                            // it's actually totally missing...
                            modifiers = modifiers.with(Modifier.OPTIONAL);
                        }
                        requires.add(new DependencyInfo(depName, modifiers,
                                depAccesses.getOrDefault(depName, Map.of())));
                    });
        } else {
            // try automatic-module?
            flags = flags.with(ModuleDescriptor.Modifier.AUTOMATIC);
            ManifestAttributes manifestAttributes = contentTree.getManifestAttributes();
            if (manifestAttributes != null) {
                mainClass = manifestAttributes.mainClassName();
                manifestAttributes.addOpens().forEach((mn, pl) -> {
                    Map<String, PackageAccess> subMap = depAccesses.computeIfAbsent(mn, ModularitySteps::newMap);
                    pl.forEach(pn -> subMap.put(pn, PackageAccess.OPEN));
                });
                manifestAttributes.addExports().forEach((mn, pl) -> {
                    Map<String, PackageAccess> subMap = depAccesses.computeIfAbsent(mn, ModularitySteps::newMap);
                    pl.forEach(pn -> subMap.putIfAbsent(pn, PackageAccess.EXPORTED));
                });
                if (manifestAttributes.enableNativeAccess()) {
                    flags = flags.with(ModuleDescriptor.Modifier.NATIVE_ACCESS);
                }
            }
            indexPackages(packages, contentTree);
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
            requires.add(new DependencyInfo("java.base", Modifier.set(Modifier.LINKED,
                    Modifier.READ, Modifier.SERVICES, Modifier.MANDATED), Map.of()));
            requires.add(new DependencyInfo("java.se", Modifier.set(Modifier.LINKED,
                    Modifier.READ, Modifier.SERVICES, Modifier.SYNTHETIC), Map.of()));
            // compute automatic dependencies
            autoDeps = computeAutomaticDependencies(moduleName, dependency,
                    allDeps, depAccesses);
        }
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

    private record WorkListItem(String moduleName, ResolvedDependency artifact, int depth, boolean optional) {
    }

    private static List<AutoDependencyGroup> computeAutomaticDependencies(String moduleName, ResolvedDependency rootArtifact,
            final Map<ArtifactKey, ResolvedDependency> allDeps,
            final Map<String, Map<String, PackageAccess>> depAccesses) {
        HashSet<ArtifactKey> visited = new HashSet<>();
        // ignore any cyclic reference
        visited.add(keyOf(rootArtifact));
        final ArrayDeque<WorkListItem> workList = new ArrayDeque<>();
        ArrayList<AutoDependencyGroup> groups = new ArrayList<>();
        workList.add(new WorkListItem(moduleName, rootArtifact, 0, false));
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
     * @param moduleName the name of the module being built (must not be {@code null})
     * @param artifact the artifact of the module being built (must not be {@code null})
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
                Modifiers<Modifier> mods = Modifier.set(Modifier.LINKED, Modifier.READ, Modifier.SERVICES);
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

    private static ArtifactKey keyOf(final ArtifactCoords dependency) {
        return ArtifactKey.gac(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier());
    }

    private static Modifiers<Modifier> depModifiersOf(final Set<AccessFlag> flags) {
        Modifiers<Modifier> set = Modifier.set(Modifier.LINKED, Modifier.READ,
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

    private static void indexPackages(final Map<String, PackageInfo> packages, final PathTree content) {
        content.walk(vp -> {
            String rp = vp.getRelativePath();
            if (rp.startsWith("META-INF/versions/")) {
                int idx = rp.indexOf('/', 18);
                if (idx == -1) {
                    // skip weird entry
                    return;
                }
                rp = rp.substring(idx + 1);
            }
            if (rp.endsWith(".class")) {
                int idx = rp.lastIndexOf('/');
                if (idx != -1) {
                    String pkgName = rp.substring(0, idx).replace('/', '.');
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

    private static <K, V> Map<K, V> newMap(Object ignored) {
        return new HashMap<>();
    }

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
