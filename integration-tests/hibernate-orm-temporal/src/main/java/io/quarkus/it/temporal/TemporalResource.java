package io.quarkus.it.temporal;

import java.time.Instant;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;

import io.quarkus.runtime.StartupEvent;

@Path("/temporal")
public class TemporalResource {

    @Inject
    SessionFactory sessionFactory;

    private volatile Instant snapshotInstant;

    @Transactional
    public void startup(@Observes StartupEvent event) throws InterruptedException {
        sessionFactory.inTransaction(session -> {
            var product = new TemporalProduct();
            product.setId(1L);
            product.setName("Quarkus T-Shirt");
            product.setPrice(25);
            session.persist(product);
        });

        Thread.sleep(100);
        snapshotInstant = Instant.now();
        Thread.sleep(100);

        sessionFactory.inTransaction(session -> {
            var product = session.find(TemporalProduct.class, 1L);
            product.setName("Quarkus Premium T-Shirt");
            product.setPrice(35);
        });
    }

    @GET
    @Path("/current/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String getCurrentProduct(@PathParam("id") long id) {
        var product = sessionFactory.fromSession(
                session -> session.find(TemporalProduct.class, id));
        return product != null ? product.getName() : "not found";
    }

    @GET
    @Path("/history/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHistoricalProduct(@PathParam("id") long id) {
        try (var session = sessionFactory.withOptions().asOf(snapshotInstant).open()) {
            var product = session.find(TemporalProduct.class, id);
            return product != null ? product.getName() : "not found";
        }
    }
}
