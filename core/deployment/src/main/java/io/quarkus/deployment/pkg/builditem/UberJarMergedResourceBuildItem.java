package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Merge duplicate resources from multiple JARs when building an Uber Jar
 */
public final class UberJarMergedResourceBuildItem extends MultiBuildItem {

    private final String path;

    public UberJarMergedResourceBuildItem(String path) {
        this.path = Assert.checkNotEmptyParam("UberJarMergedResourceBuildItem.path", path);
    }

    public String getPath() {
        return path;
    }
}
