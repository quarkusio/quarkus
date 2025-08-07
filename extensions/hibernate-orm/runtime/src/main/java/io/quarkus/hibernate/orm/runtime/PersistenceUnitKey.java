package io.quarkus.hibernate.orm.runtime;

public record PersistenceUnitKey(String name, boolean isReactive) {
}
