package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/testsORM")
@Authenticated
public class HibernateORMTestEndpoint {
    private static final Logger log = Logger.getLogger(HibernateORMTestEndpoint.class);

    @Inject
    SessionFactory sessionFactory;

    @GET
    @Path("/blockingCowPersist")
    @Transactional
    public FriesianCow reactiveCowPersist() {
        final FriesianCow cow = new FriesianCow();
        cow.name = "Carolina";

        log.info("Blocking persist, session factory:" + sessionFactory);

        Session session = (Session) sessionFactory.createEntityManager();

        session.persist(cow);
        return session.createQuery("from FriesianCow f where f.name = :name", FriesianCow.class)
                .setParameter("name", cow.name).getSingleResult();
    }

    /**
     * This test is returning a Uni, but it's using Hibernate ORM, not reactive
     *
     * @return
     */
    @GET
    @Path("/blockingCowPersistReturningUni")
    @Transactional
    public Uni<FriesianCow> reactiveCowPersistReturningUni() {
        final FriesianCow cow = new FriesianCow();
        cow.name = "Carolina returning Uni";

        log.info("Blocking persist, session factory:" + sessionFactory);

        Session session = (Session) sessionFactory.createEntityManager();

        session.persist(cow);
        FriesianCow name = session.createQuery("from FriesianCow f where f.name = :name", FriesianCow.class)
                .setParameter("name", cow.name).getSingleResult();

        return Uni.createFrom().item(name);
    }

}
