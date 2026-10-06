package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class InvalidMethodEntity {

    @ReflectionFreeAccessor
    public void invalidMethod(String a, String b) {
    }
}
