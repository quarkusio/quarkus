package io.quarkus.devui.deployment;

import java.net.URL;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * All mvnpm jars used by Dev UI
 */
public final class MvnpmBuildItem extends SimpleBuildItem {
    private final Set<URL> mvnpmJars;

    public MvnpmBuildItem(Set<URL> mvnpmJars) {
        this.mvnpmJars = mvnpmJars;
    }

    public Set<URL> getMvnpmJars() {
        return mvnpmJars;
    }
}
