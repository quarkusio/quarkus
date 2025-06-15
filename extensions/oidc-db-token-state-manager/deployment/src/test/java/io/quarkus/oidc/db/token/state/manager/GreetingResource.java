package io.quarkus.oidc.db.token.state.manager;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/greeting")
@Authenticated
public class GreetingResource {

    @Inject
    EntityManager em;

    @Transactional
    @Path("/new")
    @GET
    public void newGreeting() {
        var entity = new GreetingEntity();
        entity.greeting = Objects.requireNonNull("Good day");
        em.persist(entity);
    }

    @GET
    public Object getGreetings() {
        return em.createNativeQuery("SELECT greeting FROM Greeting").getResultList().get(0);
    }
}
