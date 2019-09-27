package io.quarkus.it.jpa.elementcollection;

import java.time.DayOfWeek;
import java.time.LocalTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
