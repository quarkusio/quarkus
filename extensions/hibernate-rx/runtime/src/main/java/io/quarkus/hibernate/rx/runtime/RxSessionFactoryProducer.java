package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.rx.RxSessionFactory;

@ApplicationScoped
public class RxSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    private EntityManagerFactory emf;

    @Produces
    @Typed(RxSessionFactory.class)
    @Singleton
    public RxSessionFactory rxSessionFactory() {
        return emf.unwrap(RxSessionFactory.class);
    }

}
