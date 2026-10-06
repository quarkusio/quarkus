package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public record RecordEntity(@ReflectionFreeAccessor String name, @ReflectionFreeAccessor int age) {
}
