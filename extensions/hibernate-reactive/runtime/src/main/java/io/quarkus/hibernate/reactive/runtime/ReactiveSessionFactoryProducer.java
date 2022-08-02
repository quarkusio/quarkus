package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.reactive.common.spi.MutinyImplementor;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;

public class ReactiveSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    EntityManagerFactory emf;

    @Produces
    @ApplicationScoped
    @DefaultBean
    @Unremovable
    @Typed({ Mutiny.SessionFactory.class, MutinyImplementor.class })
    public MutinySessionFactoryImpl mutinySessionFactory() {
        // TODO Remove this cast when we get rid of the dependency to MutinyImplementor
        return (MutinySessionFactoryImpl) emf.unwrap(Mutiny.SessionFactory.class);
    }

}
