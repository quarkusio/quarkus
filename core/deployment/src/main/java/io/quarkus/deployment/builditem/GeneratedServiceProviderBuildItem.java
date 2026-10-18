package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a service provider registration to be generated during the build.
 * <p>
 * Producers of this item declare that a given {@code implementationClassName} implements
 * the given {@code serviceInterfaceName} and should be discoverable via {@link java.util.ServiceLoader}.
 * <p>
 * Packaging build steps consume this item and emit the appropriate output depending on the target:
 * <ul>
 * <li>Flat classpath: a {@code META-INF/services/<serviceInterfaceName>} resource file</li>
 * <li>JPMS module (future, see #44657): a {@code module-info} service entry</li>
 * </ul>
 */
public final class GeneratedServiceProviderBuildItem extends MultiBuildItem {

    private final String serviceInterfaceName;
    private final String implementationClassName;

    /**
     * @param serviceInterfaceName the binary name of the service interface
     *        (e.g. {@code io.quarkus.arc.ComponentsProvider})
     * @param implementationClassName the binary name of the generated implementation class
     *        (e.g. {@code io.quarkus.arc.Arc_xxx_ComponentsProviderImpl})
     */
    public GeneratedServiceProviderBuildItem(String serviceInterfaceName, String implementationClassName) {
        this.serviceInterfaceName = Objects.requireNonNull(serviceInterfaceName, "serviceInterfaceName");
        this.implementationClassName = Objects.requireNonNull(implementationClassName, "implementationClassName");
    }

    /**
     * @return the binary name of the service interface (e.g. {@code io.quarkus.arc.ComponentsProvider})
     */
    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    /**
     * @return the binary name of the generated implementation class
     */
    public String getImplementationClassName() {
        return implementationClassName;
    }
}
