package io.quarkus.modular.spi.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;

public record AutoDependencyGroup(
        String hostModuleName,
        List<DependencyInfo> dependencies) {

    public AutoDependencyGroup {
        Assert.checkNotNullParam("hostModuleName", hostModuleName);
        dependencies = List.copyOf(Assert.checkNotNullParam("dependencies", dependencies));
    }
}
