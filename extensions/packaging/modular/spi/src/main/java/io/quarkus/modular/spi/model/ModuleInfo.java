package io.quarkus.modular.spi.model;

import static io.smallrye.common.constraint.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Modifiers;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Information about a single module in the project.
 */
public record ModuleInfo(
        String name,
        String version,
        Modifiers<ModuleDescriptor.Modifier> modifiers,
        ResolvedDependency resolvedArtifact,
        String mainClassName,
        Map<String, PackageInfo> packages,
        List<DependencyInfo> dependencies,
        List<AutoDependencyGroup> autoDependencies,
        Set<String> uses,
        Map<String, List<String>> provides,
        List<Resource> generated) {

    public ModuleInfo {
        checkNotNullParam("name", name);
        checkNotNullParam("modifiers", modifiers);
        checkNotNullParam("resolvedArtifact", resolvedArtifact);
        packages = Map.copyOf(checkNotNullParam("packages", packages));
        dependencies = List.copyOf(checkNotNullParam("dependencies", dependencies));
        autoDependencies = List.copyOf(checkNotNullParam("autoDependencies", autoDependencies));
        uses = Set.copyOf(checkNotNullParam("uses", uses));
        provides = deepCopy(checkNotNullParam("provides", provides), List::copyOf);
        generated = List.copyOf(checkNotNullParam("generated", generated));
    }

    private <K, V> Map<K, V> deepCopy(final Map<K, V> map, final UnaryOperator<V> copier) {
        return Map.copyOf(map.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), copier.apply(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public ArtifactKey key() {
        // todo: cache (this is one reason why records are difficult)
        return ArtifactKey.ga(resolvedArtifact.getGroupId(), resolvedArtifact.getArtifactId());
    }

    public ModuleInfo withModifier(ModuleDescriptor.Modifier modifier) {
        if (modifiers.contains(modifier)) {
            return this;
        } else {
            return new ModuleInfo(
                    name,
                    version,
                    modifiers.with(modifier),
                    resolvedArtifact,
                    mainClassName,
                    packages,
                    dependencies,
                    autoDependencies,
                    uses,
                    provides,
                    generated);
        }
    }

    public ModuleInfo withMainClass(final String mainClassName) {
        if (Objects.equals(mainClassName, this.mainClassName)) {
            return this;
        }
        return new ModuleInfo(
                name,
                version,
                modifiers,
                resolvedArtifact,
                mainClassName,
                packages,
                dependencies,
                autoDependencies,
                uses,
                provides,
                generated);
    }

    public ModuleInfo withMorePackages(final Map<String, PackageInfo> morePackages) {
        if (morePackages.isEmpty()) {
            return this;
        }
        return new ModuleInfo(
                name,
                version,
                modifiers,
                resolvedArtifact,
                mainClassName,
                Stream.concat(packages.entrySet().stream(), morePackages.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, PackageInfo::mergedWith)),
                dependencies,
                autoDependencies,
                uses,
                provides,
                generated);
    }

    public ModuleInfo withMoreServices(final Map<String, List<String>> moreServices) {
        if (moreServices.isEmpty()) {
            return this;
        }
        return new ModuleInfo(
                name,
                version,
                modifiers,
                resolvedArtifact,
                mainClassName,
                packages,
                dependencies,
                autoDependencies,
                uses,
                Stream.concat(provides.entrySet().stream(), moreServices.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (a, b) -> Stream.concat(a.stream(), b.stream()).toList())),
                generated);
    }

    public ModuleInfo withMoreDependencies(final List<DependencyInfo> moreDependencies) {
        if (moreDependencies.isEmpty()) {
            return this;
        }
        // index the new dependencies
        Map<String, DependencyInfo> index1 = dependencies.stream()
                .collect(Collectors.toMap(DependencyInfo::moduleName, UnaryOperator.identity()));
        Map<String, DependencyInfo> index2 = moreDependencies.stream()
                .collect(Collectors.toMap(DependencyInfo::moduleName, UnaryOperator.identity()));
        // now emit the dependencies in order
        ArrayList<DependencyInfo> newDeps = new ArrayList<>(index1.size() + index2.size());
        for (DependencyInfo dep : dependencies) {
            if (index2.containsKey(dep.moduleName())) {
                // merge it
                newDeps.add(DependencyInfo.merge(dep, index2.get(dep.moduleName())));
            } else {
                // add it
                newDeps.add(dep);
            }
        }
        for (DependencyInfo dep : moreDependencies) {
            if (index1.containsKey(dep.moduleName())) {
                // we already added it; skip
            } else {
                newDeps.add(dep);
            }
        }
        return new ModuleInfo(
                name,
                version,
                modifiers,
                resolvedArtifact,
                mainClassName,
                packages,
                newDeps,
                autoDependencies,
                uses,
                provides,
                generated);
    }

    public ModuleInfo withMoreResources(final List<Resource> moreResources) {
        if (moreResources.isEmpty()) {
            return this;
        }
        return new ModuleInfo(
                name,
                version,
                modifiers,
                resolvedArtifact,
                mainClassName,
                packages,
                dependencies,
                autoDependencies,
                uses,
                provides,
                Stream.concat(generated.stream(), moreResources.stream()).toList());
    }
}
