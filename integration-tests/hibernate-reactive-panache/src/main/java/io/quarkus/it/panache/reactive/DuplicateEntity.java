package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

@Entity
public class DuplicateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public static <T extends PanacheEntityBase> Uni<T> findById(Object id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = (Integer) id;
        return (Uni<T>) Uni.createFrom().item(duplicate);
    }

    @Override
    public Uni<Void> persist() {
        return Uni.createFrom().nullItem();
    }

    public static Uni<Integer> update(String query, Parameters params) {
        return Uni.createFrom().item(0);
    }
}
