package io.quarkus.hibernate.orm.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Singleton
public class DefaultEntityManagerProducer {

    @Produces
    @PersistenceContext
    EntityManager entityManager;

}
