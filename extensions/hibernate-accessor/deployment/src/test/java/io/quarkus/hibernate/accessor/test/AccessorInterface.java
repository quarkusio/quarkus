package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public interface AccessorInterface {

    @ReflectionFreeAccessor
    String getLabel();
}
