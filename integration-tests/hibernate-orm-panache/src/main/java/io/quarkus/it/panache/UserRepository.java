package io.quarkus.it.panache;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
@Transactional
public class UserRepository implements PanacheRepositoryBase<User, String> {

    public Optional<User> find(final String id) {
        return Optional.ofNullable(findById(id));
    }
}
