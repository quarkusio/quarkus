package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.rx.RxSessionFactory;

@ApplicationScoped
public class RxSessionProducer {

    @Inject
    @RxSession
    private RxSessionFactory rxSessionFactory;

    @Produces
    @RxSession
    @Singleton
    public org.hibernate.rx.RxSession rxSession() {
        System.out.println("@AGG producing RxSession with rxSF=" + rxSessionFactory);
        return rxSessionFactory.openRxSession();
    }

}
