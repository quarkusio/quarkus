package io.quarkus.hibernate.orm.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

@Singleton
public class DefaultEntityManagerFactoryProducer {

    @Produces
    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

}
