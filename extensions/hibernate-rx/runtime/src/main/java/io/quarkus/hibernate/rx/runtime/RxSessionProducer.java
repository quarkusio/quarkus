package io.quarkus.hibernate.rx.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hibernate.rx.RxSessionFactory;
import org.hibernate.rx.mutiny.Mutiny;

@ApplicationScoped
public class RxSessionProducer {

    @Inject
    @RxSession
    private RxSessionFactory rxSessionFactory;

    @Produces
    @RxSession
    @RequestScoped
    public org.hibernate.rx.RxSession rxSession() {
        System.out.println("@AGG producing RxSession with rxSF=" + rxSessionFactory);
        return rxSessionFactory.openRxSession();
    }

    @Produces
    @RxSession
    @RequestScoped
    public Mutiny.Session mutinySession(@RxSession org.hibernate.rx.RxSession rxSession) {
        System.out.println("@AGG producing Mutiny.Session with rxSess=" + rxSession);
        return new Mutiny.Session(rxSession);
    }

    public void disposeRxSession(@Disposes @RxSession org.hibernate.rx.RxSession rxSession) {
        rxSession.close();
    }

    public void disposeMutinySession(@Disposes @RxSession Mutiny.Session mutinySession) {
        // TODO: Should we add a mutinySession.close() method to the API?
    }

}
