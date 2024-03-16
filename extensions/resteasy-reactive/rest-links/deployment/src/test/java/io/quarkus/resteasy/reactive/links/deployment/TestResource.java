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

    private static final List<TestRecordWithIdAndPersistenceIdAndRestLinkId> ID_AND_PERSISTENCE_ID_AND_REST_LINK_IDS = new LinkedList<>(
            Arrays.asList(
                    new TestRecordWithIdAndPersistenceIdAndRestLinkId(100, 10, 1, "One"),
                    new TestRecordWithIdAndPersistenceIdAndRestLinkId(101, 11, 2, "Two")));

    private static final List<TestRecordWithPersistenceIdAndRestLinkId> PERSISTENCE_ID_AND_REST_LINK_IDS = new LinkedList<>(
            Arrays.asList(
                    new TestRecordWithPersistenceIdAndRestLinkId(100, 10, "One"),
                    new TestRecordWithPersistenceIdAndRestLinkId(101, 11, "Two")));

    private static final List<TestRecordWithIdAndPersistenceId> ID_AND_PERSISTENCE_ID_RECORDS = new LinkedList<>(
            Arrays.asList(
                    new TestRecordWithIdAndPersistenceId(10, 1, "One"),
                    new TestRecordWithIdAndPersistenceId(11, 2, "Two")));

    private static final List<TestRecordWithIdAndRestLinkId> ID_AND_REST_LINK_ID_RECORDS = new LinkedList<>(
            Arrays.asList(
                    new TestRecordWithIdAndRestLinkId(100, 1, "One"),
                    new TestRecordWithIdAndRestLinkId(101, 2, "Two")));

    private static final List<TestRecordWithPersistenceId> PERSISTENCE_ID_RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecordWithPersistenceId(10, "One"),
            new TestRecordWithPersistenceId(11, "Two")));

    private static final List<TestRecordWithRestLinkId> REST_LINK_ID_RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecordWithRestLinkId(100, "One"),
            new TestRecordWithRestLinkId(101, "Two")));

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

    @GET
    @Path("/slugOrId/{slugOrId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestLink(rel = "get-by-slug-or-id")
    public TestRecord getBySlugOrId(@PathParam("slugOrId") String slugOrId) {
        return RECORDS.stream()
                .filter(record -> record.getSlug().equals(slugOrId)
                        || slugOrId.equalsIgnoreCase(String.valueOf(record.getId())))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-id-and-persistence-id-and-rest-link-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithIdAndPersistenceIdAndRestLinkId.class)
    @InjectRestLinks
    public TestRecordWithIdAndPersistenceIdAndRestLinkId getWithIdAndPersistenceIdAndRestLinkId(@PathParam("id") int id) {
        return ID_AND_PERSISTENCE_ID_AND_REST_LINK_IDS.stream()
                .filter(record -> record.getRestLinkId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-persistence-id-and-rest-link-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithPersistenceIdAndRestLinkId.class)
    @InjectRestLinks
    public TestRecordWithPersistenceIdAndRestLinkId getWithPersistenceIdAndRestLinkId(@PathParam("id") int id) {
        return PERSISTENCE_ID_AND_REST_LINK_IDS.stream()
                .filter(record -> record.getRestLinkId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-id-and-persistence-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithIdAndPersistenceId.class)
    @InjectRestLinks
    public TestRecordWithIdAndPersistenceId getWithIdAndPersistenceId(@PathParam("id") int id) {
        return ID_AND_PERSISTENCE_ID_RECORDS.stream()
                .filter(record -> record.getPersistenceId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-id-and-rest-link-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithIdAndRestLinkId.class)
    @InjectRestLinks
    public TestRecordWithIdAndRestLinkId getWithIdAndRestLinkId(@PathParam("id") int id) {
        return ID_AND_REST_LINK_ID_RECORDS.stream()
                .filter(record -> record.getRestLinkId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-persistence-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithPersistenceId.class)
    @InjectRestLinks
    public TestRecordWithPersistenceId getWithPersistenceId(@PathParam("id") int id) {
        return PERSISTENCE_ID_RECORDS.stream()
                .filter(record -> record.getPersistenceId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/with-rest-link-id/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(entityType = TestRecordWithRestLinkId.class)
    @InjectRestLinks
    public TestRecordWithRestLinkId getWithRestLinkId(@PathParam("id") int id) {
        return REST_LINK_ID_RECORDS.stream()
                .filter(record -> record.getRestLinkId() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

}
