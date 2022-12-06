package io.quarkus.resteasy.reactive.links.deployment;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Uni;

@Path("/records")
public class TestResource {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private static final List<TestRecord> RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecord(ID_COUNTER.incrementAndGet(), "first", "First value"),
            new TestRecord(ID_COUNTER.incrementAndGet(), "second", "Second value")));

    @GET
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecord.class)
    @InjectRestLinks
    public Uni<List<TestRecord>> getAll() {
        return Uni.createFrom().item(RECORDS).onItem().delayIt().by(Duration.ofMillis(100));
    }

    @GET
    @Path("/without-links")
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink(rel = "list-without-links")
    public List<TestRecord> getAllWithoutLinks() {
        return RECORDS;
    }

    @GET
    @Path("/{id: \\d+}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecord.class)
    @InjectRestLinks(RestLinkType.INSTANCE)
    public TestRecord getById(@PathParam("id") int id) {
        return RECORDS.stream()
                .filter(record -> record.getId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{slug: [a-zA-Z-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink(rel = "get-by-slug")
    @InjectRestLinks(RestLinkType.INSTANCE)
    public TestRecord getBySlug(@PathParam("slug") String slug) {
        return RECORDS.stream()
                .filter(record -> record.getSlug().equals(slug))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }
}
