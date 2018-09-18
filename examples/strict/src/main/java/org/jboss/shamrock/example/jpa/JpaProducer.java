package org.jboss.shamrock.example.jpa;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

public class JpaProducer {

    @Produces
    @PersistenceUnit(unitName = "templatePU")
    EntityManagerFactory emf;

    @Produces
    @PersistenceContext(unitName = "templatePU")
    EntityManager em;
}
