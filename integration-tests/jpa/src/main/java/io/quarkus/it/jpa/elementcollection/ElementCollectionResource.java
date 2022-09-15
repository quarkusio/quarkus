package io.quarkus.it.jpa.elementcollection;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/element-collection")
@ApplicationScoped
public class ElementCollectionResource {

    @Inject
    EntityManager em;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String create() {
        OpeningTimes openingTimes = new OpeningTimes("standard", LocalTime.of(10, 0), LocalTime.of(19, 0), DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        em.persist(openingTimes);

        return "OK";
    }
}
