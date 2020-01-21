package io.quarkus.hibernate.orm.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class DefaultEntityManagerProducer {

    @Produces
    @PersistenceContext
    @Singleton
    EntityManager entityManager;

}
