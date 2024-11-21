package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/tests")
@Authenticated
public class HibernateReactiveTestEndpoint {

    private static final Logger log = Logger.getLogger(HibernateReactiveTestEndpoint.class);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @GET
    @Path("/reactiveCowPersist")
    public Uni<FriesianCow> reactiveCowPersist() {
        final FriesianCow cow = new FriesianCow();
        cow.name = "Carolina Reactive";

        log.info("Reactive persist, session factory:" + sessionFactory);

        return sessionFactory
                .withTransaction(s -> s.persist(cow))
                .chain(() -> sessionFactory
                        .withSession(s -> s.createQuery("from FriesianCow f where f.name = :name", FriesianCow.class)
                                .setParameter("name", cow.name).getSingleResult()));
    }
}
