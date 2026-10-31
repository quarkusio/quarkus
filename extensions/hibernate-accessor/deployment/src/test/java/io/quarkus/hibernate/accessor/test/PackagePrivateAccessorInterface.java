package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

interface PackagePrivateAccessorInterface {

    @ReflectionFreeMethodAccessor
    String getLabel();
}
