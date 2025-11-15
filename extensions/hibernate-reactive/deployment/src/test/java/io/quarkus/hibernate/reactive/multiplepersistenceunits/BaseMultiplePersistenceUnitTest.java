package io.quarkus.hibernate.reactive.multiplepersistenceunits;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.user.User;
import io.smallrye.mutiny.Uni;

public class BaseMultiplePersistenceUnitTest {
    protected Uni<Plane> fetchPlane(Mutiny.SessionFactory sessionFactory) {
        return sessionFactory
                .withTransaction(session -> session.createQuery("select p from Plane p where p.name = :name", Plane.class)
                        .setParameter("name", "plane")
                        .getSingleResult());
    }

    protected Uni<User> fetchUser(Mutiny.SessionFactory sessionFactory) {
        return sessionFactory
                .withTransaction(session -> session.createQuery("select u from User u where u.name = :name", User.class)
                        .setParameter("name", "user")
                        .getSingleResult());
    }
}
