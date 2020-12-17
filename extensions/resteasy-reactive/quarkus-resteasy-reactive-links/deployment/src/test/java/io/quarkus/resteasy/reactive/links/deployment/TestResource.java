package io.quarkus.resteasy.reactive.links.deployment;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;

@Path("/records")
public class TestResource {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private static final List<TestRecord> RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecord(ID_COUNTER.incrementAndGet(), "first", "First value"),
            new TestRecord(ID_COUNTER.incrementAndGet(), "second", "Second value")));

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink(entityType = TestRecord.class, rel = "list")
    @InjectRestLinks
    public List<TestRecord> getAll() {
        return RECORDS;
    }

    @GET
    @Path("/without-links")
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink
    public List<TestRecord> getAllWithoutLinks() {
        return RECORDS;
    }

    @GET
    @Path("/{id: \\d+}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink(entityType = TestRecord.class, rel = "self")
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
    @RestLink
    @InjectRestLinks(RestLinkType.INSTANCE)
    public TestRecord getBySlug(@PathParam("slug") String slug) {
        return RECORDS.stream()
                .filter(record -> record.getSlug().equals(slug))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }
}
