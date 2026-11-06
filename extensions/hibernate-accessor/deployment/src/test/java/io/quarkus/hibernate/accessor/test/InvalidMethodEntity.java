package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

public class InvalidMethodEntity {

    @GeneratedMethodAccessor
    public void invalidMethod(String a, String b) {
    }
}
