package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DuplicateRepository implements PanacheRepositoryBase<DuplicateEntity, Integer> {

    @Override
    public Uni<DuplicateEntity> findById(Integer id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = id;
        return Uni.createFrom().item(duplicate);
    }
}
