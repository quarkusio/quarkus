package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Ignore resources when building an Uber Jar
 */
public final class UberJarIgnoredResourceBuildItem extends MultiBuildItem {

    private final String path;

    public UberJarIgnoredResourceBuildItem(String path) {
        this.path = Assert.checkNotEmptyParam("UberJarIgnoredResourceBuildItem.path", path);
    }

    public String getPath() {
        return path;
    }
}
