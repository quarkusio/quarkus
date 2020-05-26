package io.quarkus.hibernate.reactive.runtime;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ReactiveSessionProducer {

    @Inject
    private Stage.SessionFactory reactiveSessionFactory;

    @Inject
    private Mutiny.SessionFactory mutinySessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    public CompletionStage<Stage.Session> stageSession() {
        return reactiveSessionFactory.openSession();
    }

    @Produces
    @RequestScoped
    @DefaultBean
    public Uni<Mutiny.Session> mutinySession() {
        return mutinySessionFactory.openSession();
    }

    public void disposeStageSession(@Disposes CompletionStage<Stage.Session> reactiveSession) {
        reactiveSession.whenComplete((s, t) -> {
            if (s != null)
                s.close();
        });
    }

    public void disposeMutinySession(@Disposes Uni<Mutiny.Session> reactiveSession) {
    	// TODO: @AGG this is not working, need to check w/ Clement to figure out the proper way
        reactiveSession.on().termination((session, ex, cancelled) -> {
            if (session != null)
                session.close();
        });
    }

}
