package io.quarkus.hibernate.validator.spi;

import java.util.function.BiPredicate;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem to replace the default traversable resolver
 */
public final class BeanValidationTraversableResolverBuildItem extends SimpleBuildItem {

    private final BiPredicate<Object, String> attributeLoadedPredicate;

    public BeanValidationTraversableResolverBuildItem(BiPredicate<Object, String> attributeLoadedPredicate) {
        this.attributeLoadedPredicate = attributeLoadedPredicate;
    }

    public BiPredicate<Object, String> getAttributeLoadedPredicate() {
        return attributeLoadedPredicate;
    }
}
