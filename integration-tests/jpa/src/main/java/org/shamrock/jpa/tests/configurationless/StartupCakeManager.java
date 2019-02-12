package org.shamrock.jpa.tests.configurationless;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.shamrock.runtime.StartupEvent;

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
