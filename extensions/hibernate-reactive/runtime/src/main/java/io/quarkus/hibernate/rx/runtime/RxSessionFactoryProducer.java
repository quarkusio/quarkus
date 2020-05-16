package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.rx.RxSessionFactory;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class RxSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    EntityManagerFactory emf;

    @Produces
    @Singleton
    @DefaultBean
    @Typed(RxSessionFactory.class)
    public RxSessionFactory rxSessionFactory() {
        return emf.unwrap(RxSessionFactory.class);
    }

}
