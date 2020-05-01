package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.rx.RxSessionFactory;

@ApplicationScoped
public class RxSessionFactoryProducer {

    //    private volatile RxSessionFactory rxSessionFactory;
    //
    //    void initialize(RxSessionFactory rxSessionFactory) {
    //        this.rxSessionFactory = rxSessionFactory;
    //    }

    @Inject
    @PersistenceUnit
    private EntityManagerFactory emf;

    @Produces
    @RxSession
    @Singleton
    public RxSessionFactory rxSessionFactory() {
        System.out.println("@AGG producing RxSessionFactory emf=" + emf);
        return emf.unwrap(RxSessionFactory.class);
    }

}
