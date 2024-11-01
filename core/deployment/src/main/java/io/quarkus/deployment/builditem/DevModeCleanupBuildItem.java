package io.quarkus.deployment.builditem;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This allows you to define one or more packages or set of classes that should be removed on Prod Build
 */
public final class DevModeCleanupBuildItem extends MultiBuildItem {
    private final Set<Class> classes;
    private final Set<String> packageNames;

    public DevModeCleanupBuildItem(Class c) {
        this(c, false);
    }

    public DevModeCleanupBuildItem(Class c, boolean wholePackage) {
        this(Set.of(c), wholePackage);
    }

    public DevModeCleanupBuildItem(Set<Class> c, boolean wholePackage) {
        super();
        if (wholePackage) {
            this.classes = null;
            this.packageNames = c.stream()
                    .map(clazz -> clazz.getPackage().getName())
                    .collect(Collectors.toSet());
        } else {
            this.classes = c;
            this.packageNames = null;
        }
    }

    public DevModeCleanupBuildItem(Set<Class> classes) {
        super();
        this.classes = classes;
        this.packageNames = null;
    }

    public DevModeCleanupBuildItem(String... packageName) {
        super();
        this.packageNames = Set.of(packageName);
        this.classes = null;

    }

    public Set<String> getPackageNames() {
        return packageNames;
    }

    public Set<Class> getClasses() {
        return classes;
    }

    public boolean isWholePackage() {
        return this.classes == null;
    }

}
