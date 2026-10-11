package io.quarkus.hibernate.validator.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem to replace the default traversable resolver
 */
public final class BeanValidationTraversableResolverBuildItem extends SimpleBuildItem {

    private final AttributeLoadedPredicate attributeLoadedPredicate;

    public BeanValidationTraversableResolverBuildItem(AttributeLoadedPredicate attributeLoadedPredicate) {
        this.attributeLoadedPredicate = attributeLoadedPredicate;
    }

    public AttributeLoadedPredicate getAttributeLoadedPredicate() {
        return attributeLoadedPredicate;
    }
}
