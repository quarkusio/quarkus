package io.quarkus.hibernate.validator.runtime;

import java.lang.annotation.ElementType;

import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import jakarta.validation.TraversableResolver;

class TraverseAllTraversableResolver implements TraversableResolver {

    TraverseAllTraversableResolver() {
    }

    @Override
    public boolean isReachable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType) {
        return true;
    }

    @Override
    public boolean isCascadable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType) {
        return true;
    }
}
