package io.quarkus.arc.deployment;

import java.util.Collection;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows extensions to programmatically exclude certain packages from split package detection which is executed by
 * {@link SplitPackageProcessor}. Extensions are encouraged to solve split package issues and this build item should
 * be used primarily as temporary workaround.
 * <p>
 * A package string representation can be:
 * <ul>
 * <li>a full name of the package, i.e. {@code org.acme.foo}</li>
 * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, which matches a package that starts with provided
 * value</li>
 *
 */
public final class IgnoreSplitPackageBuildItem extends MultiBuildItem {

    private Collection<String> excludedPackages;

    public IgnoreSplitPackageBuildItem(Collection<String> excludedPackages) {
        this.excludedPackages = excludedPackages;
    }

    public Collection<String> getExcludedPackages() {
        return excludedPackages;
    }
}
