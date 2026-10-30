package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

public class InvalidMethodEntity {

    @ReflectionFreeMethodAccessor
    public void invalidMethod(String a, String b) {
    }
}
