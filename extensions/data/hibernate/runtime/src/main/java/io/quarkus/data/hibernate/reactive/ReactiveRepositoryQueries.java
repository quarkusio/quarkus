package io.quarkus.data.hibernate.reactive;

import java.util.List;

import jakarta.data.metamodel.Attribute;

import io.quarkus.data.hibernate.RepositoryQueries;
import io.smallrye.mutiny.Uni;

public interface ReactiveRepositoryQueries<Entity, Id>
        extends
        RepositoryQueries<Uni<Entity>, Uni<List<Entity>>, ReactiveDataQuery<Entity>, Uni<Long>, Uni<Boolean>, Id> {

    @Override
    default <T> ReactiveDataQuery<Entity> find(Attribute<T> attribute, Object value) {
        return value == null ? find(attribute.name() + " IS NULL") : find(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> Uni<List<Entity>> list(Attribute<T> attribute, Object value) {
        return value == null ? list(attribute.name() + " IS NULL") : list(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> Uni<Long> count(Attribute<T> attribute, Object value) {
        return value == null ? count(attribute.name() + " IS NULL") : count(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> Uni<Long> delete(Attribute<T> attribute, Object value) {
        return value == null ? delete(attribute.name() + " IS NULL") : delete(attribute.name() + " = ?1", value);
    }
}
