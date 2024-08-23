package io.quarkus.hibernate.reactive.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;

@Path("contextTest")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ContextFruitResource {

    @Inject
    Mutiny.SessionFactory sf;

    @GET
    @Path("valid")
    public Uni<Fruit> get() {
        return sf.withTransaction((s, t) -> s.find(Fruit.class, 1));
    }

    @GET
    @Path("invalid")
    public Uni<Fruit> getSingle(@RestPath Integer id) {
        VertxContextSafetyToggle.setCurrentContextSafe(false);
        return get();
    }

}
