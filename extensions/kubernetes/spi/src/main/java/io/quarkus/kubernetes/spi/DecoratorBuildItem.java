
package io.quarkus.kubernetes.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that wraps around Decorator objects.
 * The purpose of those build items is to perform modification on the generated resources.
 */
public final class DecoratorBuildItem extends MultiBuildItem {

    /**
     * The group the decorator will be applied to.
     */
    private final String group;

    /**
     * The decorator
     */
    private final Object decorator;

    public DecoratorBuildItem(Object decorator) {
        this(null, decorator);
    }

    public DecoratorBuildItem(String group, Object decorator) {
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
