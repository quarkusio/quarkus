package io.quarkus.it.panache.reactive;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

@Entity
public class DuplicateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public static Uni<DuplicateEntity> findById(Object id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = (Integer) id;
        return Uni.createFrom().item(duplicate);
    }

    @Override
    public Uni<Void> persist() {
        return Uni.createFrom().nullItem();
    }

    public static Uni<Integer> update(String query, Parameters params) {
        return Uni.createFrom().item(0);
    }
}
