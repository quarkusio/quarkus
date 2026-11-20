package io.quarkus.modular.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.modular.spi.items.ClassAccessBuildItem;
import io.quarkus.modular.spi.items.GeneratedClassPlacementBuildItem;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency.Modifier;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Resolves class-based contributions using the final packaging ownership of generated classes.
 * The same ownership decision must drive both access grants and the placement of class bytes: granting access
 * to one module while packaging the referenced class in another would leave the generated code unusable.
 *
 * <p>
 * The constructor collects the classes needed by access and placement declarations, including anchors and generated
 * siblings in explicitly placed packages. It uses the package inventory shared with module assembly to narrow candidate
 * artifacts, then checks all requested classes in each artifact together, keeping its content tree open for the batch.
 * Only these class ownership results are retained; unrelated dependency classes are not indexed.
 * Generated ownership is resolved separately, in this order:
 * <ol>
 * <li>An explicit placement selects the application module or recursively follows its anchor class. An anchor can itself
 * be generated. A generated replacement of an existing class must stay in that class's original module.</li>
 * <li>Otherwise, an existing class belongs to the unique artifact containing that class, even if its package is split
 * across artifacts.</li>
 * <li>A generated class without explicit placement follows the unique owner of its package. If no artifact owns that
 * package, it follows explicit placements of other generated classes in the same package. If neither source supplies
 * an owner, it belongs to the application module.</li>
 * <li>A class absent from both the artifact index and generated classes is looked up in the JDK's system modules.
 * The class resource must exist; an unknown class is an error, not an application-module default.</li>
 * </ol>
 * Resolved owners are cached. The recursion path detects anchor cycles, and multiple candidate owners are rejected
 * rather than choosing an artifact arbitrarily. After resolving explicit placements, the constructor checks that they
 * do not introduce split packages and adds newly claimed packages to the package index. Placement into an existing
 * owner of an already split package is allowed. Generated classes cannot be
 * placed in JDK modules.
 *
 * <p>
 * Next, the constructor resolves the caller and target of each access declaration using those ownership rules.
 * Same-module access needs no grant. Cross-module direct access adds linkage, readability and a qualified export;
 * reflective access adds readability and a qualified open without linkage. Requirements are merged by caller module
 * and target module, with opens taking precedence over exports. JDK classes can be targets, but cannot be callers
 * because this resolver only contributes dependencies to application and dependency modules.
 *
 * <p>
 * Byte placement happens later, in {@link #claimResources}, after generated resources have been overlaid with
 * transformations and removals. It claims the remaining bytes of generated classes in explicitly placed packages,
 * including siblings without their own placement when the package has a unique owner, and generated classes already resolved as
 * access endpoints or
 * anchors. These bytes leave the legacy package-based routing map and are grouped by their resolved module.
 * Unrelated generated content keeps the existing packaging behavior; removed content is not recreated.
 *
 * <p>
 * During module assembly, {@link #packages} supplies the packages needed by the claimed classes and {@link #contribute}
 * adds their bytes and the merged access requirements. {@link #hasReachableGeneratedClass} keeps a destination module
 * alive during tree shaking when one of its claimed classes is reachable, even if none of its original classes are.
 * This prevents packaging from moving a generated class after its access requirements have already been resolved.
 * When there are no placement or access declarations, the resolver does no indexing and contributes nothing.
 */
final class ClassModuleResolver {
    private final String applicationModule;
    private final Map<String, Set<String>> originalOwners = new HashMap<>();
    private final Map<String, Set<String>> packageOwners = new HashMap<>();
    private final Map<String, GeneratedClassBuildItem> generated = new HashMap<>();
    private final Map<String, GeneratedClassPlacementBuildItem> placements = new HashMap<>();
    private final Map<String, List<String>> placementsByPackage = new HashMap<>();
    private final Map<String, String> resolved = new HashMap<>();
    private final Map<String, List<Resource>> resources = new HashMap<>();
    private final Map<String, Map<String, PackageInfo>> packages = new HashMap<>();
    private final Map<String, Map<String, DependencyInfo>> accesses = new HashMap<>();

    ClassModuleResolver(ResolvedDependency application, Collection<ResolvedDependency> dependencies,
            ModuleContentIndex contentIndex,
            List<GeneratedClassBuildItem> generatedClasses, List<GeneratedClassPlacementBuildItem> placements,
            List<ClassAccessBuildItem> accessItems) {
        applicationModule = application.getModuleName();
        if (placements.isEmpty() && accessItems.isEmpty()) {
            return;
        }
        for (GeneratedClassBuildItem item : generatedClasses) {
            generated.put(item.binaryName(), item);
        }
        for (GeneratedClassPlacementBuildItem item : placements) {
            String requirement = "module placement for " + item.generatedClass();
            requireNamedPackage(item.generatedClass(), requirement);
            if (item.anchorClass() != null) {
                requireNamedPackage(item.anchorClass(), requirement);
            }
            if (!generated.containsKey(item.generatedClass())) {
                throw new IllegalArgumentException(
                        "Placement refers to a class that was not generated: " + item.generatedClass());
            }
            GeneratedClassPlacementBuildItem previous = this.placements.putIfAbsent(item.generatedClass(), item);
            if (previous != null && !Objects.equals(previous.anchorClass(), item.anchorClass())) {
                throw new IllegalArgumentException("Conflicting module placements for " + item.generatedClass());
            }
        }
        for (String name : this.placements.keySet()) {
            placementsByPackage.computeIfAbsent(packageName(name), ignored -> new ArrayList<>()).add(name);
        }
        Map<String, Set<String>> requestedByPackage = new HashMap<>();
        for (GeneratedClassPlacementBuildItem item : placements) {
            requestClass(requestedByPackage, item.generatedClass());
            if (item.anchorClass() != null) {
                requestClass(requestedByPackage, item.anchorClass());
            }
        }
        for (ClassAccessBuildItem item : accessItems) {
            String requirement = item.kind() + " access from " + item.callerClass() + " to " + item.targetClass();
            requireNamedPackage(item.callerClass(), requirement);
            requireNamedPackage(item.targetClass(), requirement);
            requestClass(requestedByPackage, item.callerClass());
            requestClass(requestedByPackage, item.targetClass());
        }
        // claimResources may resolve unplaced siblings later. Include them now so replacement checks and
        // exact ownership in split packages never require reopening an artifact for individual classes.
        for (String name : generated.keySet()) {
            if (placementsByPackage.containsKey(packageName(name))) {
                requestClass(requestedByPackage, name);
            }
        }
        for (ResolvedDependency dependency : dependencies) {
            index(dependency, contentIndex, requestedByPackage);
        }
        index(application, contentIndex, requestedByPackage);
        for (String name : this.placements.keySet()) {
            moduleOf(name);
        }
        // Explicit placement cannot introduce a split package while legacy package-based resource routing is in use.
        for (String name : this.placements.keySet()) {
            String owner = moduleOf(name);
            if (JdkModules.names.contains(owner)) {
                throw new IllegalArgumentException("Cannot place generated class " + name + " in JDK module " + owner);
            }
            String pkg = packageName(name);
            Set<String> existing = packageOwners.computeIfAbsent(pkg, ignored -> new HashSet<>());
            if (!existing.isEmpty() && !existing.contains(owner)) {
                throw new IllegalArgumentException("Placement of " + name + " in " + owner
                        + " would split package " + pkg + " with " + existing);
            }
            existing.add(owner);
        }
        for (ClassAccessBuildItem item : accessItems) {
            String caller = moduleOf(item.callerClass());
            if (JdkModules.names.contains(caller)) {
                throw new IllegalArgumentException("Cannot declare class access from JDK caller " + item.callerClass());
            }
            String target = moduleOf(item.targetClass());
            if (caller.equals(target)) {
                continue;
            }
            boolean direct = item.kind() == ClassAccessBuildItem.Kind.DIRECT;
            accesses.computeIfAbsent(caller, ignored -> new HashMap<>())
                    .merge(target, new DependencyInfo(target,
                            direct ? Modifier.Set.of(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC)
                                    : Modifier.Set.of(Modifier.READ, Modifier.SYNTHETIC),
                            Map.of(packageName(item.targetClass()), direct ? PackageAccess.EXPORTED : PackageAccess.OPEN)),
                            DependencyInfo::merge);
        }
    }

    private static void requireNamedPackage(String name, String requirement) {
        if (packageName(name).isEmpty()) {
            throw new IllegalArgumentException("Class " + name + " in the default package cannot be used by " + requirement);
        }
    }

    private static void requestClass(Map<String, Set<String>> requestedByPackage, String name) {
        requestedByPackage.computeIfAbsent(packageName(name), ignored -> new HashSet<>()).add(name);
    }

    private void index(ResolvedDependency dependency, ModuleContentIndex contentIndex,
            Map<String, Set<String>> requestedByPackage) {
        String module = dependency.getModuleName();
        for (String name : contentIndex.findClasses(dependency, requestedByPackage)) {
            originalOwners.computeIfAbsent(name, ignored -> new HashSet<>()).add(module);
        }
        for (String pkg : contentIndex.packages(dependency)) {
            packageOwners.computeIfAbsent(pkg, ignored -> new HashSet<>()).add(module);
        }
    }

    String moduleOf(String name) {
        return resolve(name, new LinkedHashSet<>());
    }

    private String resolve(String name, Set<String> visiting) {
        String cached = resolved.get(name);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(name)) {
            throw new IllegalArgumentException("Cyclic generated class placement: " + visiting + " -> " + name);
        }
        String owner;
        GeneratedClassPlacementBuildItem placement = placements.get(name);
        if (placement != null) {
            owner = placement.anchorClass() == null ? applicationModule : resolve(placement.anchorClass(), visiting);
            Set<String> originals = originalOwners.getOrDefault(name, Set.of());
            if (!originals.isEmpty() && !originals.equals(Set.of(owner))) {
                throw new IllegalArgumentException("Generated class " + name + " cannot replace a class in " + originals
                        + " from module " + owner);
            }
        } else if (originalOwners.containsKey(name)) {
            owner = uniqueOwner(name, originalOwners.get(name));
        } else if (generated.containsKey(name)) {
            Set<String> owners = packageOwners.getOrDefault(packageName(name), Set.of());
            if (!owners.isEmpty()) {
                owner = uniqueOwner(name, owners);
            } else {
                // An explicit placement can introduce a package into a dependency. Legacy generated content
                // in that package must follow the same owner as packaging will choose.
                Set<String> placedOwners = new HashSet<>();
                for (String placed : placementsByPackage.getOrDefault(packageName(name), List.of())) {
                    placedOwners.add(resolve(placed, visiting));
                }
                owner = placedOwners.isEmpty() ? applicationModule : uniqueOwner(name, placedOwners);
            }
        } else {
            owner = JdkModules.moduleOf(name);
            if (owner == null) {
                throw new IllegalArgumentException("Cannot resolve runtime module for class " + name);
            }
        }
        visiting.remove(name);
        resolved.put(name, owner);
        return owner;
    }

    private static String uniqueOwner(String name, Set<String> owners) {
        if (owners.size() != 1) {
            throw new IllegalArgumentException("Ambiguous runtime module for class " + name + ": " + owners);
        }
        return owners.iterator().next();
    }

    /** Remove placement-assigned content from legacy package routing, preserving transformations and removals. */
    void claimResources(Map<String, Map<String, Resource>> generatedByPackageAndPath) {
        for (String name : generated.keySet()) {
            String pkg = packageName(name);
            // Unrelated siblings in a pre-existing split package must retain their legacy routing.
            if (!resolved.containsKey(name)
                    && (!placementsByPackage.containsKey(pkg) || packageOwners.getOrDefault(pkg, Set.of()).size() != 1)) {
                continue;
            }
            Map<String, Resource> byPath = generatedByPackageAndPath.get(pkg);
            if (byPath == null) {
                continue;
            }
            Resource resource = byPath.remove(name.replace('.', '/') + ".class");
            if (byPath.isEmpty()) {
                generatedByPackageAndPath.remove(pkg);
            }
            if (resource != null) {
                String module = moduleOf(name);
                resources.computeIfAbsent(module, ignored -> new ArrayList<>()).add(resource);
                packages.computeIfAbsent(module, ignored -> new HashMap<>()).put(pkg, PackageInfo.PRIVATE);
            }
        }
    }

    Map<String, PackageInfo> packages(String module) {
        return packages.getOrDefault(module, Map.of());
    }

    ModuleInfo contribute(ModuleInfo module) {
        return module.withMoreResources(resources.getOrDefault(module.name(), List.of()).stream()
                .sorted(Comparator.comparing(Resource::pathName)).toList())
                .withMoreDependencies(accesses.getOrDefault(module.name(), Map.of()).values().stream()
                        .sorted(Comparator.comparing(DependencyInfo::moduleName)).toList());
    }

    boolean hasReachableGeneratedClass(String module, Set<String> reachable) {
        // Preserve legacy routing for unrelated classes, including existing split packages. Only placement
        // declarations and generated access endpoints have ownership that must survive tree shaking.
        return resources.getOrDefault(module, List.of()).stream().map(Resource::pathName)
                .map(path -> path.substring(0, path.length() - 6).replace('/', '.'))
                .anyMatch(reachable::contains);
    }

    /** Lazily index system module packages; verify individual class resources without loading classes. */
    private static final class JdkModules {
        private static final Map<String, ModuleReference> byPackage = new HashMap<>();
        private static final Set<String> names = new HashSet<>();

        static {
            for (ModuleReference module : ModuleFinder.ofSystem().findAll()) {
                names.add(module.descriptor().name());
                for (String pkg : module.descriptor().packages()) {
                    byPackage.put(pkg, module);
                }
            }
        }

        private static String moduleOf(String name) {
            ModuleReference module = byPackage.get(packageName(name));
            if (module == null) {
                return null;
            }
            try (var reader = module.open()) {
                return reader.find(name.replace('.', '/') + ".class").isPresent() ? module.descriptor().name() : null;
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot inspect JDK module " + module.descriptor().name(), e);
            }
        }
    }

    private static String packageName(String name) {
        int separator = name.lastIndexOf('.');
        return separator == -1 ? "" : name.substring(0, separator);
    }
}
