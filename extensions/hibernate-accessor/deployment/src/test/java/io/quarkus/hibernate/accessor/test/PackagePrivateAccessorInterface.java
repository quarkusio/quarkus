package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

interface PackagePrivateAccessorInterface {

    @GeneratedMethodAccessor
    String getLabel();
}
