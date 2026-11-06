package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

public interface AccessorInterface {

    @GeneratedMethodAccessor
    String getLabel();
}
