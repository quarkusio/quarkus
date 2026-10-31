package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

@ReflectionFreeAccessor
public record RecordEntity(String name, int age) {
}
