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

    // TODO: I think this isn't working because we are producing Mutiny.Session as a bean but it has no default ctor
    // so one is being generated
    //    @Produces
    //    @Typed(Mutiny.Session.class)
    //    @RequestScoped
    //    public Mutiny.Session mutinySession(RxSession rxSession) {
    //        return new Mutiny.Session(rxSession);
    //    }

    public void disposeRxSession(@Disposes RxSession rxSession) {
        rxSession.close();
    }

    //    public void disposeMutinySession(@Disposes Mutiny.Session mutinySession) {
    //        // TODO: Should we add a mutinySession.close() method to the API?
    //    }

}
