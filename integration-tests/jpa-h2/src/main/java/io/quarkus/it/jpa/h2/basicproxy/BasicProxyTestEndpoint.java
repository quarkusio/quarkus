package io.quarkus.it.jpa.h2.basicproxy;

import java.io.IOException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Path("/jpa-h2/testbasicproxy")
@Produces(MediaType.TEXT_PLAIN)
public class BasicProxyTestEndpoint {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void setup(@Observes StartupEvent startupEvent) {
        ConcreteEntity entity = new ConcreteEntity();
        entity.id = "1";
        entity.type = "Concrete";
        entityManager.persist(entity);
    }

    @GET
    public String test() throws IOException {
        final List list = entityManager.createQuery("from ConcreteEntity").getResultList();
        if (list.size() != 1) {
            throw new RuntimeException("Expected 1 result, got " + list.size());
        }
        return "OK";
    }
}
