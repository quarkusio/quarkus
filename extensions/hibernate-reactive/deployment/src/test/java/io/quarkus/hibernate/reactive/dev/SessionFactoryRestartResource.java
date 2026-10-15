package io.quarkus.hibernate.reactive.dev;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("session-factory-restart")
@Produces(MediaType.APPLICATION_JSON)
public class SessionFactoryRestartResource {

    @Inject
    Mutiny.Session session;

    @Inject
    Mutiny.StatelessSession statelessSession;

    @ConfigProperty(name = "restart.counter", defaultValue = "0")
    int restartCounter;

    @POST
    @Path("stateful")
    @Transactional
    public Uni<Map<String, Object>> stateful() {
        return session.persist(new Fruit("stateful-" + restartCounter))
                .chain(() -> session.createNamedQuery("Fruits.count", Long.class).getSingleResult())
                .map(count -> Map.of("restartCounter", restartCounter, "fruitCount", count));
    }

    @POST
    @Path("stateless")
    @Transactional
    public Uni<Map<String, Object>> stateless() {
        return statelessSession.insert(new Fruit("stateless-" + restartCounter))
                .chain(() -> statelessSession.createNamedQuery("Fruits.count", Long.class).getSingleResult())
                .map(count -> Map.of("restartCounter", restartCounter, "fruitCount", count));
    }
}
