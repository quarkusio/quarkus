
package io.quarkus.kubernetes.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that wraps around Decorator objects.
 * The purpose of those build items is to perform modification on the resources to be deployed.
 * Note: The changes will not be reflected in the generated files, the resources will be modified directly on deployment.
 * This is the main difference between this and {@link DeploymentDecoratorBuildItem}.
 */
public final class DeploymentDecoratorBuildItem extends MultiBuildItem {

    /**
     * The group the decorator will be applied to.
     */
    private final String group;

    /**
     * The decorator
     */
    private final Object decorator;

    public DeploymentDecoratorBuildItem(Object decorator) {
        this(null, decorator);
    }

    public DeploymentDecoratorBuildItem(String group, Object decorator) {
        this.group = group;
        this.decorator = decorator;
    }

    public String getGroup() {
        return this.group;
    }

    public Object getDecorator() {
        return this.decorator;
    }

    public boolean matches(Class type) {
        return type.isInstance(decorator);

    }

    public <D> Optional<D> getDecorator(Class<D> type) {
        if (matches(type)) {
            return Optional.<D> of((D) decorator);
        }
        return Optional.empty();
    }
}
