package io.quarkus.it.jpa;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

@Dependent
public class JpaProducer {

    @Produces
    @PersistenceUnit(unitName = "templatePU")
    EntityManagerFactory emf;

    @Produces
    @PersistenceContext
    EntityManager em;
}
