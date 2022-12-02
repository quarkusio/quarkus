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
import io.quarkus.hibernate.orm.runtime.JPAConfig;

public class ReactiveSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    EntityManagerFactory emf;

    @Inject
    JPAConfig jpaConfig;

    @Produces
    @ApplicationScoped
    @DefaultBean
    @Unremovable
    @Typed({ Mutiny.SessionFactory.class, MutinyImplementor.class })
    public MutinySessionFactoryImpl mutinySessionFactory() {
        if (jpaConfig.getDeactivatedPersistenceUnitNames()
                .contains(HibernateReactive.DEFAULT_REACTIVE_PERSISTENCE_UNIT_NAME)) {
            throw new IllegalStateException(
                    "Cannot retrieve the Mutiny.SessionFactory for persistence unit "
                            + HibernateReactive.DEFAULT_REACTIVE_PERSISTENCE_UNIT_NAME
                            + ": Hibernate Reactive was deactivated through configuration properties");
        }
        // TODO Remove this cast when we get rid of the dependency to MutinyImplementor
        return (MutinySessionFactoryImpl) emf.unwrap(Mutiny.SessionFactory.class);
    }

}
