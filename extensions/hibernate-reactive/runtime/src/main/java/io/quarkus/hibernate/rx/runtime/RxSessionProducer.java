package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import org.hibernate.rx.RxSession;
import org.hibernate.rx.RxSessionFactory;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class RxSessionProducer {

    @Inject
    private RxSessionFactory rxSessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    @Typed(RxSession.class)
    public RxSession rxSession() {
        return rxSessionFactory.openRxSession();
    }

    public void disposeRxSession(@Disposes RxSession rxSession) {
        rxSession.close();
    }

}
