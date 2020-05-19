package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import org.hibernate.reactive.stage.Stage;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ReactiveSessionProducer {

    @Inject
    private Stage.SessionFactory reactiveSessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    @Typed(Stage.Session.class)
    public Stage.Session reactiveSession() {
        return reactiveSessionFactory.openReactiveSession();
    }

    public void disposeReactiveSession(@Disposes Stage.Session reactiveSession) {
        reactiveSession.close();
    }

}
