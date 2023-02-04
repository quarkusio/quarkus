package io.quarkus.it.jpa.configurationless;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

/**
 * creates a chocolate cake on startup to make sure JPA works in the startup event
 */
@Dependent
public class StartupCakeManager {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void startup(@Observes StartupEvent startupEvent) {
        Cake c = new Cake();
        c.setType("Chocolate");
        entityManager.persist(c);
    }
}
