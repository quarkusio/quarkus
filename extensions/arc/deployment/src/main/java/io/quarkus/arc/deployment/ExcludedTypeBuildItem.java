package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to specify types to be excluded from discovery.
 * <p>
 * An element value can be:
 * <ul>
 * <li>a fully qualified class name, i.e. {@code org.acme.Foo}</li>
 * <li>a simple class name as defined by {@link Class#getSimpleName()}, i.e. {@code Foo}</li>
 * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, matches a package</li>
 * <li>a package name with suffix {@code .**}, i.e. {@code org.acme.**}, matches a package that starts with the
 * value</li>
 * </ul>
 * If any element value matches a discovered type then the type is excluded from discovery, i.e. no beans and observer
 * methods are created from this type.
 */
public final class ExcludedTypeBuildItem extends MultiBuildItem {

    private final String match;

    public ExcludedTypeBuildItem(String match) {
        this.match = match;
    }

    public String getMatch() {
        return match;
    }
}
