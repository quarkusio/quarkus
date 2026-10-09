package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

interface PackagePrivateAccessorInterface {

    @ReflectionFreeAccessor
    String getLabel();
}
