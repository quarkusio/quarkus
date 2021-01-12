package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.quarkus.arc.DefaultBean;

public class ReactiveSessionProducer {

    @Inject
    Stage.SessionFactory reactiveSessionFactory;

    @Inject
    Mutiny.SessionFactory mutinySessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    public Stage.Session createStageSession() {
        return reactiveSessionFactory.openSession();
    }

    @Produces
    @RequestScoped
    @DefaultBean
    public Mutiny.Session createMutinySession() {
        return mutinySessionFactory.openSession();
    }

    public void disposeStageSession(@Disposes Stage.Session reactiveSession) {
        if (reactiveSession != null) {
            reactiveSession.close();
        }
    }

    public void disposeMutinySession(@Disposes Mutiny.Session reactiveSession) {
        if (reactiveSession != null) {
            reactiveSession.close();
        }
    }
}
