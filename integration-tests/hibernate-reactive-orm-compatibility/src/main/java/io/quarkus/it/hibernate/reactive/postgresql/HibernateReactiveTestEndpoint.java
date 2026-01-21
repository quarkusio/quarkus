package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
                .withTransaction(s -> s.persist(cow)
                        .chain(v -> findCowByName(s, cow).getSingleResult()));
    }

    @Inject
    Mutiny.Session session;

    @GET
    @Path("/reactiveCowPersistTransactional")
    public Uni<FriesianCow> reactiveCowPersistTransactional() {
        final FriesianCow cow = new FriesianCow();
        cow.name = "Carolina Reactive Transactional";

        log.info("Reactive persist with @Transactional, session factory:" + sessionFactory);

        return persistAndGetTransactional(session, cow);
    }

    @Transactional
    public Uni<FriesianCow> persistAndGetTransactional(Mutiny.Session session, FriesianCow cow) {
        return session.persist(cow)
                .chain(v -> findCowByName(session, cow).getSingleResult());
    }

    private static Mutiny.SelectionQuery<FriesianCow> findCowByName(Mutiny.Session session, FriesianCow cow) {
        return session.createQuery("from FriesianCow f where f.name = :name", FriesianCow.class)
                .setParameter("name", cow.name);
    }
}
