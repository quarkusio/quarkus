package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedAccessor;

@GeneratedAccessor
public record RecordEntity(String name, int age) {
}
