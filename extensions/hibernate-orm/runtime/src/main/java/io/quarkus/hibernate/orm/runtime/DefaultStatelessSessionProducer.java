package io.quarkus.hibernate.orm.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

public class DefaultStatelessSessionProducer {

    @Inject
    EntityManagerFactory em;

    @Produces
    @Singleton
    StatelessSession produceStatelessSession() {
        return em.unwrap(SessionFactory.class).openStatelessSession();
    }

}
