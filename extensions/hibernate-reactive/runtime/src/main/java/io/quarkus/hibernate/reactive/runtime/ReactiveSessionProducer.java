package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.DefaultBean;

public class ReactiveSessionProducer {

    @Inject
    Mutiny.SessionFactory mutinySessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    public Mutiny.Session createMutinySession() {
        return mutinySessionFactory.openSession();
    }

    public void disposeMutinySession(@Disposes Mutiny.Session reactiveSession) {
        if (reactiveSession != null) {
            //We're ignoring the returned CompletionStage!
            //This could certainly done better but it should be effective enough to
            //eventually close the session: the connection pool will order the operations
            //as it won't be able to return connections to other consumers when it's saturated,
            //ensuring close operations are handled.
            //N.B. for the Mutiny API we need to make sure to subscribe
            reactiveSession.close().subscribe().asCompletionStage().join();
        }
    }
}
