package io.quarkus.it.panache;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class DuplicateRepository implements PanacheRepositoryBase<DuplicateEntity, Integer> {

    @Override
    public DuplicateEntity findById(Integer id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = id;
        return duplicate;
    }
}
