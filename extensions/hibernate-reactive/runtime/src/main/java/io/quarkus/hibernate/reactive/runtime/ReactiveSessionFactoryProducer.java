package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.quarkus.arc.DefaultBean;

public class ReactiveSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    EntityManagerFactory emf;

    @Produces
    @Singleton
    @DefaultBean
    @Typed(Stage.SessionFactory.class)
    public Stage.SessionFactory reactiveSessionFactory() {
        return emf.unwrap(Stage.SessionFactory.class);
    }

    @Produces
    @Singleton
    @DefaultBean
    @Typed(Mutiny.SessionFactory.class)
    public Mutiny.SessionFactory mutinySessionFactory() {
        return emf.unwrap(Mutiny.SessionFactory.class);
    }

}
