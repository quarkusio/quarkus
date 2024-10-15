package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

@Path("/tests")
@Authenticated
public class HibernateReactiveTestEndpoint {

    private static final Logger log = Logger.getLogger(HibernateReactiveTestEndpoint.class);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    // Injecting a Vert.x Pool is not required, it's only used to
    // independently validate the contents of the database for the test
    @Inject
    PgPool pgPool;

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
