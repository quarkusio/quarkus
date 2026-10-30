package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

public interface AccessorInterface {

    @ReflectionFreeMethodAccessor
    String getLabel();
}
