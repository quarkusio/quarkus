package io.quarkus.modular.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassModel;

/**
 * Build-local package inventory shared by class ownership resolution and module assembly.
 * Descriptor package metadata avoids walking an artifact; otherwise its packages are discovered once.
 * Class queries are grouped by package and checked in one open tree per candidate artifact. Only the
 * requested class names are returned, never an inventory of every class in the runtime dependencies.
 */
final class ModuleContentIndex {
    private final Map<ResolvedDependency, ClassModel> descriptors;
    private final Map<ResolvedDependency, Set<String>> packages = new HashMap<>();

    ModuleContentIndex(Map<ResolvedDependency, ClassModel> descriptors) {
        this.descriptors = descriptors;
    }

    Set<String> packages(ResolvedDependency dependency) {
        Set<String> known = knownPackages(dependency);
        if (known != null) {
            return known;
        }
        return discoverPackages(dependency, dependency.getContentTree());
    }

    Set<String> findClasses(ResolvedDependency dependency, Map<String, Set<String>> requestedByPackage) {
        Set<String> known = knownPackages(dependency);
        if (known != null && known.stream().noneMatch(requestedByPackage::containsKey)) {
            return Set.of();
        }
        // Keep the archive open for the entire batch, including package discovery when necessary.
        try (var content = dependency.getContentTree().open()) {
            if (known == null) {
                known = discoverPackages(dependency, content);
            }
            Set<String> found = new HashSet<>();
            for (String pkg : known) {
                for (String name : requestedByPackage.getOrDefault(pkg, Set.of())) {
                    // PathTree selects the effective multi-release resource for this Java version.
                    if (content.contains(name.replace('.', '/') + ".class")) {
                        found.add(name);
                    }
                }
            }
            return found;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot inspect classes in " + dependency, e);
        }
    }

    private Set<String> knownPackages(ResolvedDependency dependency) {
        Set<String> known = packages.get(dependency);
        if (known == null) {
            ClassModel descriptor = descriptors.get(dependency);
            if (descriptor != null) {
                var attribute = descriptor.findAttribute(Attributes.modulePackages());
                if (attribute.isPresent()) {
                    known = new HashSet<>();
                    for (var pkg : attribute.get().packages()) {
                        known.add(pkg.name().stringValue().replace('/', '.'));
                    }
                    known = Set.copyOf(known);
                    packages.put(dependency, known);
                }
            }
        }
        return known;
    }

    private Set<String> discoverPackages(ResolvedDependency dependency, PathTree content) {
        Set<String> discovered = new HashSet<>();
        content.walk(visit -> {
            String name = visit.getResourceName();
            // Flatten MR-JAR structure, as module assembly did before sharing this inventory.
            if (name.startsWith("META-INF/versions/")) {
                int separator = name.indexOf('/', 18);
                if (separator == -1) {
                    // Skip weird entries.
                    return;
                }
                name = name.substring(separator + 1);
            }
            // A directory is a package if it contains a class.
            if (name.endsWith(".class")) {
                int separator = name.lastIndexOf('/');
                if (separator != -1) {
                    discovered.add(name.substring(0, separator).replace('/', '.'));
                }
            }
        });
        Set<String> result = Set.copyOf(discovered);
        packages.put(dependency, result);
        return result;
    }
}
