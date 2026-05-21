package io.quarkus.modular.spi.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.PackageAccess;

public record DependencyInfo(
        String moduleName,
        Dependency.Modifier.Set modifiers,
        Map<String, PackageAccess> packageAccesses) {
    public DependencyInfo {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("modifiers", modifiers);
        packageAccesses = Map.copyOf(Assert.checkNotNullParam("packageAccesses", packageAccesses));
    }

    public static DependencyInfo merge(DependencyInfo a, DependencyInfo b) {
        String moduleName = a.moduleName;
        if (!moduleName.equals(b.moduleName)) {
            throw new IllegalArgumentException("Cannot merge dependencies with different module names");
        }
        // merge modifiers but non-synthetic takes priority over synthetic
        Dependency.Modifier.Set modifiers = a.modifiers.xor(Dependency.Modifier.SYNTHETIC)
                .withAll(b.modifiers.xor(Dependency.Modifier.SYNTHETIC))
                .xor(Dependency.Modifier.SYNTHETIC);
        Map<String, PackageAccess> packageAccesses = Stream.concat(
                a.packageAccesses.entrySet().stream(),
                b.packageAccesses.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, PackageAccess::max));
        return new DependencyInfo(moduleName, modifiers, packageAccesses);
    }
}
