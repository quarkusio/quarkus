package io.quarkus.it.panache;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
@Transactional
public class UserRepository implements PanacheRepositoryBase<User, String> {

    public Optional<User> find(final String id) {
        return Optional.ofNullable(findById(id));
    }
}
